/*
 *  Copyright 2006-2017 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.testdata;

import java.io.File;

import java.security.KeyPair;
import java.security.KeyStore;

import java.security.cert.X509Certificate;

import java.util.Vector;

import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyStoreVerifier;
import org.webpki.crypto.MACAlgorithms;

import org.webpki.json.JSONAsymKeySigner;
import org.webpki.json.JSONAsymKeyVerifier;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONSigner;
import org.webpki.json.JSONSymKeySigner;
import org.webpki.json.JSONSymKeyVerifier;
import org.webpki.json.JSONX509Signer;
import org.webpki.json.JSONX509Verifier;

import org.webpki.util.ArrayUtil;

/*
 * Create JCS test vectors
 */
public class Signatures {
    static String baseKey;
    static String baseSignatures;
    static SymmetricKeys symmetricKeys;
    static JSONX509Verifier x509Verifier;
    static String keyId;
   
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new Exception("Wrong number of arguments");
        }
        CustomCryptoProvider.forcedLoad(true);
        baseKey = args[0] + File.separator;
        baseSignatures = args[1] + File.separator;
        symmetricKeys = new SymmetricKeys(baseKey);
        
        X509Certificate rootca = JSONParser.parse(ArrayUtil.readFile(baseKey + "rootca.jcer"))
                .getJSONArrayReader().getCertificatePath()[0];
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load (null, null);
        keyStore.setCertificateEntry ("mykey", rootca);        
        x509Verifier = new JSONX509Verifier(new KeyStoreVerifier(keyStore));

       
        asymSign("p256");
        asymSign("p384");
        asymSign("p521");
        asymSign("r2048");

        asymSignNoPublicKeyInfo("p256", true);
        asymSignNoPublicKeyInfo("p521", false);

        certSign("p256");
        certSign("p384");
        certSign("p521");
        certSign("r2048");
        
        symmSign(256, MACAlgorithms.HMAC_SHA256);
        symmSign(384, MACAlgorithms.HMAC_SHA384);
        symmSign(512, MACAlgorithms.HMAC_SHA512);
        
        multipleSign("p256", "r2048");
    }
    
    static void symmSign(int keyBits, MACAlgorithms algorithm) throws Exception {
        byte[] key = symmetricKeys.getValue(keyBits);
        String keyName = symmetricKeys.getName(keyBits);
        byte[] signedData = createSignature(new JSONSymKeySigner(key, algorithm).setKeyId(keyName));
        ArrayUtil.writeFile(baseSignatures + "hs" + (key.length * 8) + "signed.json", signedData);
        JSONParser.parse(signedData).getSignature(new JSONSignatureDecoder.Options()
                .setRequirePublicKeyInfo(false)
                .setKeyIdOption(JSONSignatureDecoder.KEY_ID_OPTIONS.REQUIRED)).verify(new JSONSymKeyVerifier(key));
    }

    static String getDataToSign() throws Exception {
        return new String(ArrayUtil.readFile(baseSignatures + "datatobesigned.json"), 
                          "UTF-8").replace("\r", "");
    }
    
    static JSONObjectWriter parseDataToSign() throws Exception {
        return new JSONObjectWriter(JSONParser.parse(getDataToSign()));
    }

    static byte[] createSignature(JSONSigner signer) throws Exception {
        String signed = parseDataToSign().setSignature(signer).toString();
        int i = signed.indexOf(",\n  \"signature\":");
        String unsigned = getDataToSign();
        int j = unsigned.lastIndexOf("\n}");
        return (unsigned.substring(0,j) + signed.substring(i)).getBytes("UTF-8");
    }
    
    static byte[] createSignatures(Vector<JSONSigner> signers) throws Exception {
        String signed = parseDataToSign().setSignatures(signers).toString();
        int i = signed.indexOf(",\n  \"signatures\":");
        String unsigned = getDataToSign();
        int j = unsigned.lastIndexOf("\n}");
        return (unsigned.substring(0,j) + signed.substring(i)).getBytes("UTF-8");
    }

    static KeyPair readJwk(String keyType) throws Exception {
        JSONObjectReader jwkPlus = JSONParser.parse(ArrayUtil.readFile(baseKey + keyType + "privatekey.jwk"));
        // Note: The built-in JWK decoder does not accept "kid" since it doesn't have a meaning in JCS or JEF. 
        if ((keyId = jwkPlus.getStringConditional("kid")) != null) {
            jwkPlus.removeProperty("kid");
        }
        return jwkPlus.getKeyPair();
    }

    static void asymSign(String keyType) throws Exception {
        KeyPair keyPair = readJwk(keyType);
        byte[] signedData = createSignature(new JSONAsymKeySigner(keyPair.getPrivate(), keyPair.getPublic(), null));
        ArrayUtil.writeFile(baseSignatures + keyType + "keysigned.json", signedData);
        JSONParser.parse(signedData).getSignature(new JSONSignatureDecoder.Options()).verify(new JSONAsymKeyVerifier(keyPair.getPublic()));;
     }

    static void multipleSign(String keyType1, String KeyType2) throws Exception {
        KeyPair keyPair1 = readJwk(keyType1);
        KeyPair keyPair2 = readJwk(KeyType2);
        Vector<JSONSigner> signers = new Vector<JSONSigner>();
        signers.add(new JSONAsymKeySigner(keyPair1.getPrivate(), keyPair1.getPublic(), null));
        signers.add(new JSONAsymKeySigner(keyPair2.getPrivate(), keyPair2.getPublic(), null));
        byte[] signedData = createSignatures(signers);
        ArrayUtil.writeFile(baseSignatures + keyType1 + "+" + KeyType2 + "keysigned.json", signedData);
        Vector<JSONSignatureDecoder> signatures = 
                JSONParser.parse(signedData).getSignatures(new JSONSignatureDecoder.Options());
        signatures.get(0).verify(new JSONAsymKeyVerifier(keyPair1.getPublic()));
        signatures.get(1).verify(new JSONAsymKeyVerifier(keyPair2.getPublic()));
        if (signatures.size() != 2) {
            throw new Exception("Wrong multi");
        }
     }

    static void asymSignNoPublicKeyInfo(String keyType, boolean wantKeyId) throws Exception {
        KeyPair keyPair = readJwk(keyType);
        JSONSigner signer = 
                new JSONAsymKeySigner(keyPair.getPrivate(), keyPair.getPublic(), null)
            .setKeyId(wantKeyId ? keyId : "");
        byte[] signedData = createSignature(signer);
        ArrayUtil.writeFile(baseSignatures + keyType + "implicitkeysigned.json", signedData);
        JSONParser.parse(signedData).getSignature(
            new JSONSignatureDecoder.Options()
                .setRequirePublicKeyInfo(false)
                .setKeyIdOption(wantKeyId ? 
     JSONSignatureDecoder.KEY_ID_OPTIONS.REQUIRED : JSONSignatureDecoder.KEY_ID_OPTIONS.FORBIDDEN))
                    .verify(new JSONAsymKeyVerifier(keyPair.getPublic()));
     }

    static void certSign(String keyType) throws Exception {
        KeyPair keyPair = readJwk(keyType);
        X509Certificate[] certPath = JSONParser.parse(ArrayUtil.readFile(baseKey + keyType + "certificate.jcer"))
                .getJSONArrayReader().getCertificatePath();
        byte[] signedData = 
            createSignature(new JSONX509Signer(keyPair.getPrivate(), certPath, null)
                                .setSignatureCertificateAttributes(true));
        ArrayUtil.writeFile(baseSignatures + keyType + "certsigned.json", signedData);
        JSONParser.parse(signedData).getSignature(new JSONSignatureDecoder.Options()).verify(x509Verifier);
    }
}