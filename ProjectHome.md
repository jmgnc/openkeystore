An architecture and implementation of a keystore for cryptographic keys supporting PKI, OTP, and Information Cards.  In addition to the keystore there is a matching protocol supporting provisioning and management of user-credentials.

Project home:<br>
<a href='http://webpki.org/WebToken.html'>http://webpki.org/WebToken.html</a>

Current specification:<br>
<a href='https://openkeystore.googlecode.com/svn/resources/trunk/docs/sks-api-arch.pdf'>https://openkeystore.googlecode.com/svn/resources/trunk/docs/sks-api-arch.pdf</a>

SKS reference implementation:<br>
<a href='https://code.google.com/p/openkeystore/source/browse/library/trunk/src/org/webpki/sks/test/SKSReferenceImplementation.java'>https://code.google.com/p/openkeystore/source/browse/library/trunk/src/org/webpki/sks/test/SKSReferenceImplementation.java</a>

NOTE: currently only the "library" and "resources" projects are suitable for download and testing:<br>
$ cd library/build<br>
$ ant<br>
$ ant testkeygen2<br>
$ ant testsks<br>

UPDATE: "webpkisuite-4-android" contains a complete <code>SKS/KeyGen2</code> implementation running at "app-level" for testing/evaluation.   It is available in a ready-to-run version from <code>PlayStore</code>: <a href='https://play.google.com/store/apps/details?id=org.webpki.mobile.android'>https://play.google.com/store/apps/details?id=org.webpki.mobile.android</a>

UPDATE: "android.mod" contains a port of Xerces bringing XML Schema proficiency to Android using standard JDK methods.