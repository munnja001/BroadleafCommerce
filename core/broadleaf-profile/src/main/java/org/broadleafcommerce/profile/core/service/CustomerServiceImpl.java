/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.profile.core.service;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.email.service.EmailService;
import org.broadleafcommerce.common.email.service.info.EmailInfo;
import org.broadleafcommerce.common.security.util.PasswordChange;
import org.broadleafcommerce.common.security.util.PasswordReset;
import org.broadleafcommerce.common.security.util.PasswordUtils;
import org.broadleafcommerce.common.service.GenericResponse;
import org.broadleafcommerce.common.time.SystemTime;
import org.broadleafcommerce.profile.core.dao.CustomerDao;
import org.broadleafcommerce.profile.core.dao.CustomerForgotPasswordSecurityTokenDao;
import org.broadleafcommerce.profile.core.dao.RoleDao;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerForgotPasswordSecurityToken;
import org.broadleafcommerce.profile.core.domain.CustomerForgotPasswordSecurityTokenImpl;
import org.broadleafcommerce.profile.core.domain.CustomerRole;
import org.broadleafcommerce.profile.core.domain.CustomerRoleImpl;
import org.broadleafcommerce.profile.core.domain.Role;
import org.broadleafcommerce.profile.core.service.handler.PasswordUpdatedHandler;
import org.broadleafcommerce.profile.core.service.listener.PostRegistrationObserver;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Service("blCustomerService")
public class CustomerServiceImpl implements CustomerService {
	private static final Log LOG = LogFactory.getLog(CustomerServiceImpl.class);
	
    @Resource(name="blCustomerDao")
    protected CustomerDao customerDao;

    @Resource(name="blIdGenerationService")
    protected IdGenerationService idGenerationService;
    
    @Resource(name="blCustomerForgotPasswordSecurityTokenDao")
    protected CustomerForgotPasswordSecurityTokenDao customerForgotPasswordSecurityTokenDao;  

    @Resource(name="blPasswordEncoder")
    protected PasswordEncoder passwordEncoder;
    
    @Resource(name="blRoleDao")
    protected RoleDao roleDao;
    
    @Resource(name="blEmailService")
    protected EmailService emailService;
    
    @Resource(name="blForgotPasswordEmailInfo")
    protected EmailInfo forgotPasswordEmailInfo;

    @Resource(name="blForgotUsernameEmailInfo")
    protected EmailInfo forgotUsernameEmailInfo;    
    
    @Resource(name="blRegistrationEmailInfo")
    protected EmailInfo registrationEmailInfo;    
    
    @Resource(name="blChangePasswordEmailInfo")
    protected EmailInfo changePasswordEmailInfo;       
    
    protected int tokenExpiredMinutes = 30;
    protected int passwordTokenLength = 20;   
    		 
    protected final List<PostRegistrationObserver> postRegisterListeners = new ArrayList<PostRegistrationObserver>();
    protected List<PasswordUpdatedHandler> passwordResetHandlers = new ArrayList<PasswordUpdatedHandler>();
    protected List<PasswordUpdatedHandler> passwordChangedHandlers = new ArrayList<PasswordUpdatedHandler>();

    public Customer saveCustomer(Customer customer) {
        return saveCustomer(customer, true);
    }

    public Customer saveCustomer(Customer customer, boolean register) {
        if (register && !customer.isRegistered()) {
            customer.setRegistered(true);
        }
        if (customer.getUnencodedPassword() != null) {
            customer.setPassword(passwordEncoder.encodePassword(customer.getUnencodedPassword(), null));
        }

        // let's make sure they entered a new challenge answer (we will populate
        // the password field with hashed values so check that they have changed
        // id
        if (customer.getUnencodedChallengeAnswer() != null && !customer.getUnencodedChallengeAnswer().equals(customer.getChallengeAnswer())) {
            customer.setChallengeAnswer(passwordEncoder.encodePassword(customer.getUnencodedChallengeAnswer(), null));
        }
        return customerDao.save(customer);
    }

    public Customer registerCustomer(Customer customer, String password, String passwordConfirm) {
        customer.setRegistered(true);

        // When unencodedPassword is set the save() will encode it
        if (customer.getId() == null) {
            customer.setId(idGenerationService.findNextId("org.broadleafcommerce.profile.core.domain.Customer"));
        }
        customer.setUnencodedPassword(password);
        Customer retCustomer = saveCustomer(customer);
        Role role = roleDao.readRoleByName("ROLE_USER");
        CustomerRole customerRole = new CustomerRoleImpl();
        customerRole.setRole(role);
        customerRole.setCustomer(retCustomer);
        roleDao.addRoleToCustomer(customerRole);
        
        HashMap<String, Object> vars = new HashMap<String, Object>();
		vars.put("customer", retCustomer);
        
        emailService.sendTemplateEmail(customer.getEmailAddress(), getRegistrationEmailInfo(), vars);        
        notifyPostRegisterListeners(retCustomer);
        return retCustomer;
    }

    public Customer readCustomerByEmail(String emailAddress) {
        return customerDao.readCustomerByEmail(emailAddress);
    }

    public Customer changePassword(PasswordChange passwordChange) {
        Customer customer = readCustomerByUsername(passwordChange.getUsername());
        customer.setUnencodedPassword(passwordChange.getNewPassword());
        customer.setPasswordChangeRequired(passwordChange.getPasswordChangeRequired());
        customer = saveCustomer(customer);
        
        for (PasswordUpdatedHandler handler : passwordChangedHandlers) {
        	handler.passwordChanged(passwordChange, customer, passwordChange.getNewPassword());
        }
        
        return customer;
    }
    
	public Customer resetPassword(PasswordReset passwordReset) {
        Customer customer = readCustomerByUsername(passwordReset.getUsername());
        String newPassword = PasswordUtils.generateTemporaryPassword(passwordReset.getPasswordLength());
        customer.setUnencodedPassword(newPassword);
        customer.setPasswordChangeRequired(passwordReset.getPasswordChangeRequired());
        customer = saveCustomer(customer);
        
        for (PasswordUpdatedHandler handler : passwordResetHandlers) {
        	handler.passwordChanged(passwordReset, customer, newPassword);
        }
        
        return customer;
    }

    public void addPostRegisterListener(PostRegistrationObserver postRegisterListeners) {
        this.postRegisterListeners.add(postRegisterListeners);
    }

    public void removePostRegisterListener(PostRegistrationObserver postRegisterListeners) {
        if (this.postRegisterListeners.contains(postRegisterListeners)) {
            this.postRegisterListeners.remove(postRegisterListeners);
        }
    }

    protected void notifyPostRegisterListeners(Customer customer) {
        for (Iterator<PostRegistrationObserver> iter = postRegisterListeners.iterator(); iter.hasNext();) {
            PostRegistrationObserver listener = iter.next();
            listener.processRegistrationEvent(customer);
        }
    }

    public Customer createCustomer() {
        return createCustomerFromId(null);
    }

    public Customer createCustomerFromId(Long customerId) {
        Customer customer = customerId != null ? readCustomerById(customerId) : null;
        if (customer == null) {
            customer = customerDao.create();
            if (customerId != null) {
                customer.setId(customerId);
            } else {
                customer.setId(idGenerationService.findNextId("org.broadleafcommerce.profile.core.domain.Customer"));
            }
        }
        return customer;
    }
    
    public Customer createNewCustomer() {
        return createCustomerFromId(null);
    }

    public Customer readCustomerByUsername(String username) {
        return customerDao.readCustomerByUsername(username);
    }

    public Customer readCustomerById(Long id) {
        return customerDao.readCustomerById(id);
    }

    public void setCustomerDao(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

	public List<PasswordUpdatedHandler> getPasswordResetHandlers() {
		return passwordResetHandlers;
	}

	public void setPasswordResetHandlers(List<PasswordUpdatedHandler> passwordResetHandlers) {
		this.passwordResetHandlers = passwordResetHandlers;
	}

	public List<PasswordUpdatedHandler> getPasswordChangedHandlers() {
		return passwordChangedHandlers;
	}

	public void setPasswordChangedHandlers(List<PasswordUpdatedHandler> passwordChangedHandlers) {
		this.passwordChangedHandlers = passwordChangedHandlers;
	}
	
	public GenericResponse sendForgotUsernameNotification(String emailAddress) {
		GenericResponse response = new GenericResponse();
		List<Customer> customers = null;
		if (emailAddress != null) {
			customers = customerDao.readCustomersByEmail(emailAddress);
		}

		if (customers == null || customers.isEmpty()) {
			response.addErrorCode("notFound");
		} else {
			List<String> activeUsernames = new ArrayList<String>();
			for (Customer customer: customers) {
				if (! customer.isDeactivated()) {
					activeUsernames.add(customer.getUsername());
				}
			}

			if (activeUsernames.size() > 0) {
				HashMap<String, Object> vars = new HashMap<String, Object>();
				vars.put("userNames", activeUsernames);
				emailService.sendTemplateEmail(emailAddress, getForgotUsernameEmailInfo(), vars);
			} else {
				// send inactive username found email.
				response.addErrorCode("inactiveUser");
			}
		}
		return response;
	}

	public GenericResponse sendForgotPasswordNotification(String username, String resetPasswordUrl) {
		GenericResponse response = new GenericResponse();
		Customer customer = null;

		if (username != null) {
			customer = customerDao.readCustomerByUsername(username);
		}

		checkCustomer(customer,response);

		if (! response.getHasErrors()) {        
			String token = PasswordUtils.generateTemporaryPassword(getPasswordTokenLength());
			token = token.toLowerCase();

			CustomerForgotPasswordSecurityToken fpst = new CustomerForgotPasswordSecurityTokenImpl();
			fpst.setCustomerId(customer.getId());
			fpst.setToken(passwordEncoder.encodePassword(token,null));
			fpst.setCreateDate(SystemTime.asDate());
			customerForgotPasswordSecurityTokenDao.saveToken(fpst);

			HashMap<String, Object> vars = new HashMap<String, Object>();
			vars.put("token", token);
			if (!StringUtils.isEmpty(resetPasswordUrl)) {
				if (resetPasswordUrl.contains("?")) {
					resetPasswordUrl=resetPasswordUrl+"&token="+token;
				} else {
					resetPasswordUrl=resetPasswordUrl+"?token="+token;
				}
			}
			vars.put("resetPasswordUrl", resetPasswordUrl); 
			emailService.sendTemplateEmail(customer.getEmailAddress(), getForgotPasswordEmailInfo(), vars);
		}
		return response;
	}
	
	public GenericResponse checkPasswordResetToken(String token) {
		GenericResponse response = new GenericResponse();
        checkPasswordResetToken(token, response);               
        return response;
	}
	
	private CustomerForgotPasswordSecurityToken checkPasswordResetToken(String token, GenericResponse response) {
		if (token == null || "".equals(token)) {
            response.addErrorCode("invalidToken");
        }
		
        CustomerForgotPasswordSecurityToken fpst = null;
        if (! response.getHasErrors()) {
            token = token.toLowerCase();
            fpst = customerForgotPasswordSecurityTokenDao.readToken(passwordEncoder.encodePassword(token,null));
            if (fpst == null) {
                response.addErrorCode("invalidToken");
            } else if (fpst.isTokenUsedFlag()) {
                response.addErrorCode("tokenUsed");
            } else if (isTokenExpired(fpst)) {
                response.addErrorCode("tokenExpired");
            }
        }		
        return fpst;
	}
    
    public GenericResponse resetPasswordUsingToken(String username, String token, String password, String confirmPassword) {
        GenericResponse response = new GenericResponse();
        Customer customer = null;
        if (username != null) {
            customer = customerDao.readCustomerByUsername(username);
        }
        checkCustomer(customer, response);
        checkPassword(password, confirmPassword, response);
        CustomerForgotPasswordSecurityToken fpst = checkPasswordResetToken(token, response);
        
        if (! response.getHasErrors()) {
        	if (! customer.getId().equals(fpst.getCustomerId())) {
        		if (LOG.isWarnEnabled()) {
        			LOG.warn("Password reset attempt tried with mismatched customer and token " + customer.getId() + ", " + token);
        		}
        		response.addErrorCode("invalidToken");
        	}
        }

        if (! response.getHasErrors()) {
            customer.setUnencodedPassword(password);
            saveCustomer(customer);
            fpst.setTokenUsedFlag(true);
            customerForgotPasswordSecurityTokenDao.saveToken(fpst);
        }

        return response;    	
    }
    
    protected void checkCustomer(Customer customer, GenericResponse response) {       
        if (customer == null) {        	
            response.addErrorCode("invalidCustomer");
        } else if (customer.getEmailAddress() == null || "".equals(customer.getEmailAddress())) {
            response.addErrorCode("emailNotFound");
        } else if (customer.isDeactivated()) {
            response.addErrorCode("inactiveUser");
        }
    }
    
    protected void checkPassword(String password, String confirmPassword, GenericResponse response) {
        if (password == null || confirmPassword == null || "".equals(password) || "".equals(confirmPassword)) {
            response.addErrorCode("invalidPassword");
        } else if (! password.equals(confirmPassword)) {
            response.addErrorCode("passwordMismatch");
        }
    }

    protected boolean isTokenExpired(CustomerForgotPasswordSecurityToken fpst) {
        Date now = SystemTime.asDate();
        long currentTimeInMillis = now.getTime();
        long tokenSaveTimeInMillis = fpst.getCreateDate().getTime();
        long minutesSinceSave = (currentTimeInMillis - tokenSaveTimeInMillis)/60000;
        return minutesSinceSave > tokenExpiredMinutes;
    }

    public int getTokenExpiredMinutes() {
        return tokenExpiredMinutes;
    }

    public void setTokenExpiredMinutes(int tokenExpiredMinutes) {
        this.tokenExpiredMinutes = tokenExpiredMinutes;
    }

    public int getPasswordTokenLength() {
        return passwordTokenLength;
    }

    public void setPasswordTokenLength(int passwordTokenLength) {
        this.passwordTokenLength = passwordTokenLength;
    }


	public EmailInfo getForgotPasswordEmailInfo() {
		return forgotPasswordEmailInfo;
	}

	public void setForgotPasswordEmailInfo(EmailInfo forgotPasswordEmailInfo) {
		this.forgotPasswordEmailInfo = forgotPasswordEmailInfo;
	}

	public EmailInfo getForgotUsernameEmailInfo() {
		return forgotUsernameEmailInfo;
	}

	public void setForgotUsernameEmailInfo(EmailInfo forgotUsernameEmailInfo) {
		this.forgotUsernameEmailInfo = forgotUsernameEmailInfo;
	}

	public EmailInfo getRegistrationEmailInfo() {
		return registrationEmailInfo;
	}

	public void setRegistrationEmailInfo(EmailInfo registrationEmailInfo) {
		this.registrationEmailInfo = registrationEmailInfo;
	}

	public EmailInfo getChangePasswordEmailInfo() {
		return changePasswordEmailInfo;
	}

	public void setChangePasswordEmailInfo(EmailInfo changePasswordEmailInfo) {
		this.changePasswordEmailInfo = changePasswordEmailInfo;
	}
}
