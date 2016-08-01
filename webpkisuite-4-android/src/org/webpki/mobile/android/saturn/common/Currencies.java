/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.mobile.android.saturn.common;

import java.io.IOException;

import java.io.Serializable;

import java.math.BigDecimal;

public enum Currencies implements Serializable {

    USD ("$\u200a",       true,  2), 
    EUR ("\u2009\u20ac",  false, 2),
    GBP ("\u00a3\u200a",  true,  2);

    public String symbol;
    public boolean symbolFirst;
    int decimals;

    Currencies (String symbol, boolean symbolFirst, int decimals) {
        this.symbol = symbol;
        this.symbolFirst = symbolFirst;
        this.decimals = decimals;
    }

    public int getDecimals() {
        return decimals;
    }

    public String amountToDisplayString(BigDecimal amount) throws IOException {
        String amountString = amount.setScale(decimals).toPlainString();
        int dp = amountString.indexOf('.');
        StringBuffer amountString2 = new StringBuffer();
        for (int i = 0; i < dp; i++) {
            amountString2.append(amountString.charAt(i));
            if (i < dp - 1 && (dp - i - 1) % 3 == 0) {
                amountString2.append(',');
            }
        }
        amountString2.append(amountString.substring(dp));
        return symbolFirst ? symbol + amountString2.toString() : amountString2.toString() + symbol;
    }
}
