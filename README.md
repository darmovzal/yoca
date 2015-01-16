# YOCA PKI
## YOCA PKI - Your Own Certificate Authority for Android

Your swiss army knife for public key security.

With YOCA you can easily:
* generate strong public/private keys
* create self-signed CA certificates
* issue client certificates for:
	* web browser client authentication
	*  HTTPS server authentication (eg. Apache)
	* VPN user and server authentication (eg. OpenVPN)
	* etc.
* issue and/or sign certificate signing requests (CSR)
* verify certificates with CA certificate
* create and verify file signatures
* import and export encrypted PKCS#12 (.p12) keystores
* import and export encrypted keys, certificates and CSR's
* revoke certificates and export revocation lists (CRLs)

YOCA PKI contains support for:
* assigning basic and extended key usage and subject alternative name in certifcate extensions
* one-click archiving and restoring of all keys and certificates, archives are encrypted with AES symmetric cipher
* RSA and DSA key types
* SHA-1 and SHA-256 signature (digest) types

This application contains set of tools for asymmetric cryptography. Asymmetric cryptography uses two mathematically linked keys. Only way to decrypt data encrypted with one of them is to use the other one. One of them is declared public and is usually used for data encryption and verifying signatures. The other is declared private, protected with passphrase and is used for decryption and creating data signatures.

