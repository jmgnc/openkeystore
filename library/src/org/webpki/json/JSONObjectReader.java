/*
 *  Copyright 2006-2015 WebPKI.org (http://webpki.org).
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
package org.webpki.json;

import java.io.IOException;
import java.io.Serializable;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.security.KeyPair;
import java.security.PublicKey;

import java.security.cert.X509Certificate;

import java.util.GregorianCalendar;
import java.util.Vector;

import java.util.regex.Pattern;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.util.Base64URL;
import org.webpki.util.ISODateTime;

/**
 * JSON object reader.
 * <p>
 * Returned by the parser methods.
 * Also provides built-in support for JCS (JSON Cleartext Signatures) decoding.</p>
 *
 */
public class JSONObjectReader implements Serializable, Cloneable
  {
    private static final long serialVersionUID = 1L;

    static final Pattern DECIMAL_PATTERN = Pattern.compile ("-?([1-9][0-9]*|0)[\\.][0-9]+");
    static final Pattern INTEGER_PATTERN = Pattern.compile ("-?[1-9][0-9]*|0");

    JSONObject root;

    JSONObjectReader (JSONObject root)
      {
        this.root = root;
      }

    public void checkForUnread () throws IOException
      {
        if (getJSONArrayReader () == null)
          {
            JSONObject.checkObjectForUnread (root);
          }
        else
          {
            JSONObject.checkArrayForUnread (root.properties.get (null), "Outer");
          }
      }

    JSONValue getProperty (String name) throws IOException
      {
        JSONValue value = root.properties.get (name);
        if (value == null)
          {
            throw new IOException ("Property \"" + name + "\" is missing");
          }
        return value;
      }

    JSONValue getProperty (String name, JSONTypes expected_type) throws IOException
      {
        JSONValue value = getProperty (name);
        JSONTypes.compatibilityTest (expected_type, value);
        value.readFlag = true;
        return value;
      }

    String getString (String name, JSONTypes expected) throws IOException
      {
        JSONValue value = getProperty (name, expected);
        return (String) value.value;
      }

    public String getString (String name) throws IOException
      {
        return getString (name, JSONTypes.STRING);
      }

    static long parseLong (String value) throws IOException
      {
        if (INTEGER_PATTERN.matcher (value).matches ())
          {
            double number = Double.valueOf (value);
            if (Math.abs (number) > JSONObjectWriter.MAX_SAFE_INTEGER)
              {
                throw new IOException ("Integer values must not exceeed " + 
                                       JSONObjectWriter.MAX_SAFE_INTEGER  +
                                       ", found: " + value);
              }
            return (long) number;
          }
        throw new IOException ("Value is not an integer: " + value);
      }

    static int parseInt (String value) throws IOException
      {
        long longValue = parseLong (value);
        if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE)
          {
            throw new IOException ("Java \"int\" out of range: " + value);
          }
        return (int)longValue;
      }

    public int getInt (String name) throws IOException
      {
        return parseInt (getString (name, JSONTypes.NUMBER));
      }

    public long getLong (String name) throws IOException
      {
        return parseLong (getString (name, JSONTypes.NUMBER));
      }

    public double getDouble (String name) throws IOException
      {
        return Double.valueOf (getString (name, JSONTypes.NUMBER));
      }

    public boolean getBoolean (String name) throws IOException
      {
        return new Boolean (getString (name, JSONTypes.BOOLEAN));
      }

    public GregorianCalendar getDateTime (String name) throws IOException
      {
        return ISODateTime.parseDateTime (getString (name));
      }

    public byte[] getBinary (String name) throws IOException
      {
        return Base64URL.decode (getString (name));
      }

    static BigInteger parseBigInteger (String value) throws IOException
      {
        if (INTEGER_PATTERN.matcher (value).matches ())
          {
            return new BigInteger (value);
          }
        throw new IOException ("Malformed \"BigInteger\": " + value);
      }

    static BigDecimal parseBigDecimal (String value, Integer decimals) throws IOException
      {
        if (INTEGER_PATTERN.matcher (value).matches () ||
            DECIMAL_PATTERN.matcher (value).matches ())
          {
            BigDecimal parsed = new BigDecimal (value);
            if (decimals != null && parsed.scale () != decimals)
              {
                throw new IOException ("Incorrect number of decimals in \"BigDecimal\": " + parsed.scale ());
              }
            return parsed;
          }
        throw new IOException ("Malformed \"BigDecimal\": " + value);
      }

    public BigInteger getBigInteger (String name) throws IOException
      {
        return parseBigInteger (getString (name));
      }

    public BigDecimal getBigDecimal (String name) throws IOException
      {
        return parseBigDecimal (getString (name), null);
      }

    public BigDecimal getBigDecimal (String name, Integer decimals) throws IOException
      {
        return parseBigDecimal (getString (name), decimals);
      }

    @SuppressWarnings("unchecked")
    public JSONArrayReader getJSONArrayReader ()
      {
        return root.properties.containsKey (null) ? new JSONArrayReader ((Vector<JSONValue>) root.properties.get (null).value) : null;
      }

    public boolean getIfNULL (String name) throws IOException
      {
        if (getPropertyType (name) == JSONTypes.NULL)
          {
            scanAway (name);
            return true;
          }
        return false;
      }

    public JSONObjectReader getObject (String name) throws IOException
      {
        JSONValue value = getProperty (name, JSONTypes.OBJECT);
        return new JSONObjectReader ((JSONObject) value.value);
      }

    @SuppressWarnings("unchecked")
    public JSONArrayReader getArray (String name) throws IOException
      {
        JSONValue value = getProperty (name, JSONTypes.ARRAY);
        return new JSONArrayReader ((Vector<JSONValue>) value.value);
      }

    public String getStringConditional (String name) throws IOException
      {
        return this.getStringConditional (name, null);
      }

    public boolean getBooleanConditional (String name) throws IOException
      {
        return this.getBooleanConditional (name, false);
      }

    public String getStringConditional (String name, String default_value) throws IOException
      {
        return hasProperty (name) ? getString (name) : default_value;
      }

    public boolean getBooleanConditional (String name, boolean default_value) throws IOException
      {
        return hasProperty (name) ? getBoolean (name) : default_value;
      }

    public byte[] getBinaryConditional (String name) throws IOException
      {
        return hasProperty (name) ? getBinary (name) : null;
      }

    public String[] getStringArrayConditional (String name) throws IOException
      {
        return hasProperty (name) ? getStringArray (name) : null;
      }

    String [] getSimpleArray (String name, JSONTypes expectedType) throws IOException
      {
        Vector<String> array = new Vector<String> ();
        @SuppressWarnings("unchecked")
        Vector<JSONValue> arrayElements = ((Vector<JSONValue>) getProperty (name, JSONTypes.ARRAY).value);
        for (JSONValue value : arrayElements)
          {
            JSONTypes.compatibilityTest (expectedType, value);
            value.readFlag = true;
            array.add ((String)value.value);
          }
        return array.toArray (new String[0]);
      }

    public String[] getStringArray (String name) throws IOException
      {
        return getSimpleArray (name, JSONTypes.STRING);
      }

    public Vector<byte[]> getBinaryArray (String name) throws IOException
      {
        Vector<byte[]> blobs = new Vector<byte[]> ();
        for (String blob : getStringArray (name))
          {
            blobs.add (Base64URL.decode (blob));
          }
        return blobs;
      }

    public String[] getProperties ()
      {
        return root.properties.keySet ().toArray (new String[0]);
      }

    public boolean hasProperty (String name)
      {
        return root.properties.get (name) != null;
      }

    public JSONTypes getPropertyType (String name) throws IOException
      {
        return getProperty (name).type;
      }

    /**
     * Read and decode JCS signature object from the current JSON object.
     * @return An object which can be used to verify keys etc.
     * @see org.webpki.json.JSONObjectWriter#setSignature(JSONSigner)
     * @throws IOException In case there is something wrong with the signature 
     */
    public JSONSignatureDecoder getSignature (AlgorithmPreferences algorithmPreferences) throws IOException
      {
        return new JSONSignatureDecoder (this, algorithmPreferences);
      }

    public JSONSignatureDecoder getSignature () throws IOException
      {
        return new JSONSignatureDecoder (this, AlgorithmPreferences.JOSE_ACCEPT_PREFER);
      }
 
    public PublicKey getPublicKey (AlgorithmPreferences algorithmPreferences) throws IOException
      {
        return getObject(JSONSignatureDecoder.PUBLIC_KEY_JSON).getCorePublicKey(algorithmPreferences);
      }

    public PublicKey getPublicKey () throws IOException
      {
        return getPublicKey (AlgorithmPreferences.JOSE_ACCEPT_PREFER);
      }

    void clearReadFlags() {
        for (JSONValue value : root.properties.values()) {
            value.readFlag = false;
        }
    }

    public PublicKey getCorePublicKey(AlgorithmPreferences algorithmPreferences) throws IOException {
        clearReadFlags();
        PublicKey publicKey = JSONSignatureDecoder.decodePublicKey(this,
                                                                   algorithmPreferences, 
                                                                   JSONSignatureDecoder.TYPE_JSON,
                                                                   JSONSignatureDecoder.CURVE_JSON);
        checkForUnread ();
        return publicKey;
    }

    public PublicKey getPublicKeyFromJwk() throws IOException {
        return JSONSignatureDecoder.decodePublicKey(this,
                                                    AlgorithmPreferences.JOSE, 
                                                    JSONSignatureDecoder.JWK_KTY_JSON,
                                                    JSONSignatureDecoder.JWK_CRV_JSON);
    }

    public KeyPair getKeyPairFromJwk() throws IOException {
        PublicKey publicKey = getPublicKeyFromJwk();
        return new KeyPair(publicKey, JSONSignatureDecoder.decodeJwkPrivateKey(this, publicKey));
    }

    public JSONDecryptionDecoder getEncryptionObject() throws IOException {
        return new JSONDecryptionDecoder(this);
    }

    public X509Certificate[] getCertificatePath () throws IOException
      {
        return JSONSignatureDecoder.getCertificatePath (this);
      }

    public void scanAway (String name) throws IOException
      {
        getProperty (name, getPropertyType (name));
      }

    public JSONObjectReader removeProperty (String name) throws IOException
      {
        getProperty (name);
        root.properties.remove (name);
        return this;
      }

    public byte[] serializeJSONObject (JSONOutputFormats output_format) throws IOException
      {
        return new JSONObjectWriter (root).serializeJSONObject (output_format);
      }

    /**
     * Deep copy of JSON object
     */
    @Override
    public JSONObjectReader clone ()
      {
        try
          {
            return JSONParser.parse (serializeJSONObject (JSONOutputFormats.NORMALIZED));
          }
        catch (IOException e)
          {
            throw new RuntimeException (e);
          }
      }
 
    @Override
    public String toString ()
      {
        return new JSONObjectWriter (root).toString ();
      }
  }
