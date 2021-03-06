/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.common.money;

import java.util.HashMap;

public class CurrencyConversionContext {
	
	private static final ThreadLocal<CurrencyConversionService> currencyConversionService = new ThreadLocal<CurrencyConversionService>();

	private static final ThreadLocal<HashMap> currencyConversionContext = new ThreadLocal<HashMap>();

	public static HashMap getCurrencyConversionContext() {
		return CurrencyConversionContext.currencyConversionContext.get();
	}
	
	public static void setCurrencyConversionContext(HashMap currencyConsiderationContext) {
		CurrencyConversionContext.currencyConversionContext.set(currencyConsiderationContext);
	}
	
	public static CurrencyConversionService getCurrencyConversionService() {
		return CurrencyConversionContext.currencyConversionService.get();
	}
	
	public static void setCurrencyConversionService(CurrencyConversionService currencyDeterminationService) {
		CurrencyConversionContext.currencyConversionService.set(currencyDeterminationService);
	}

}
