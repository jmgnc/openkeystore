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
package org.webpki.tools;

import java.io.FileOutputStream;

import java.util.Enumeration;

import java.security.KeyStore;
import java.security.PublicKey;

import java.security.cert.Certificate;

import org.webpki.crypto.KeyStoreReader;
import org.webpki.crypto.CustomCryptoProvider;

import org.webpki.util.Base64;


public class KeyStore2PEMConverter
  {
    private static void fail ()
      {
        System.out.println (KeyStore2PEMConverter.class.getName () + "  keystore-file password PEM-file qualifier\n" +
                           "   qualifier = [public private certificate trust]");
        System.exit (3);
      }

    public static void main (String argv[]) throws Exception
      {
        if (argv.length < 4)
          {
            fail ();
          }
        boolean private_key = false;
        boolean public_key = false;
        boolean certificate = false;
        boolean trust = false;
        for (int i = 3; i < argv.length; i++) {
          if (argv[i].equals ("public"))
            {
              public_key = true;
            }
          else if (argv[i].equals ("private"))
            {
              private_key = true;
            }
          else if (argv[i].equals ("certificate"))
            {
              certificate = true;
            }
          else if (argv[i].equals ("trust"))
            {
              trust = true;
            }
          else
            {
              fail ();
            }
        }
        CustomCryptoProvider.forcedLoad (true);
        KeyStore ks = KeyStoreReader.loadKeyStore (argv[0], argv[1]);
        FileOutputStream fis = new FileOutputStream (argv[2]);
        Enumeration<String> aliases = ks.aliases ();
        while (aliases.hasMoreElements ())
          {
            String alias = aliases.nextElement ();
            if (ks.isKeyEntry (alias))
              {
                if (private_key)
                  {
                    writeObject (fis, "PRIVATE KEY", ks.getKey (alias, argv[1].toCharArray ()).getEncoded ());
                  }
                if (certificate) for (Certificate cert : ks.getCertificateChain (alias))
                  {
                    writeCert (fis, cert);
                  }
                if (public_key)
                  {
                    writeObject (fis, "PUBLIC KEY", ks.getCertificateChain (alias)[0].getPublicKey ().getEncoded ());
                  }
              }
            else if (ks.isCertificateEntry (alias))
              {
                if (trust)
                  {
                    writeCert (fis, ks.getCertificate (alias));
                  }
              }
            else
              {
                throw new Exception ("Bad KS");
              }
          }
      }

    private static void writeObject (FileOutputStream fis, String string, byte[] encoded) throws Exception
      {
        fis.write (("-----BEGIN " + string + "-----\n").getBytes ("UTF-8")); 
        fis.write (new Base64 ().getBase64BinaryFromBinary (encoded));
        fis.write (("\n-----END " + string + "-----\n\n").getBytes ("UTF-8")); 
      }

    private static void writeCert (FileOutputStream fis, Certificate cert) throws Exception
      {
        writeObject (fis, "CERTIFICATE", cert.getEncoded ());
      }
  }
