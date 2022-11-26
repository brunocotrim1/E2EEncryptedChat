package org.psd.ClientPSD.crypto;

import org.psd.ClientPSD.model.Share;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class SecretSharing {
    private static final BigInteger field = new BigInteger("8CF83642A709A097B447997640129DA299B1A47D1EB3750BA308B0FE64F5FBD3", 16);
    private static final SecureRandom rndGenerator = new SecureRandom();
    public static int threshold = 2;
    private static int polyDegree = threshold-1;		// = threshold - 1
    private static int nShareholders = 4;	// must be > than polyDegree

    public static Share[] generateKeyShares(String user1,String user2){
        SecretKey secretKey = generateKey();
        BigInteger secret = new BigInteger(secretKey.getEncoded());
        return share(polyDegree, nShareholders, secret,user1,user2);
    }

    public static Share[] reShareSecret(SecretKey secret, String user1, String user2){
        BigInteger secretKey = new BigInteger(secret.getEncoded());
        return share(polyDegree, nShareholders, secretKey,user1,user2);
    }

    public static Share[] generateKeyShare(SecretKey key,String user1,String user2){
        BigInteger secret = new BigInteger(Base64.getEncoder().encode(key.getEncoded()));
        return share(polyDegree, nShareholders, secret,user1,user2);
    }

    public static SecretKey combineKeyShares(Share[] shares){
        BigInteger recoveredSecret = combine(shares);
        byte [] secretBytes1 =Base64.getDecoder().decode(recoveredSecret.toByteArray());
        return new SecretKeySpec(secretBytes1, 0,secretBytes1.length, "AES");
    }

    private static SecretKey generateKey(){
        SecureRandom rnd = new SecureRandom();
        byte[] data = new byte[16]; // 16 * 8 = 128 bit
        rnd.nextBytes(data);
        BigInteger bigInt = new BigInteger(data).abs();
        return new SecretKeySpec(bigInt.toByteArray(), 0,bigInt.toByteArray().length, "AES");
    }




    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }
    public static String encrypt (String algorithm, SecretKey key, IvParameterSpec iv, String message) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String decrypt (String algorithm, SecretKey key, IvParameterSpec iv, String ciphertext) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(plainText);
    }

    /**
     * This method shares a secret using Shamir's scheme.
     * @param polyDegree Degree of the polynomial.
     * @param nShareholders Number of shareholders.
     * @param secret Secret to share.
     * @return Shares of the secret.
     */
    private static Share[] share(int polyDegree, int nShareholders, BigInteger secret,String user1,String user2) {
        //creating polynomial: P(x) = a_d * x^d + ... + a_1 * x^1 + secret | P(0) = secret
        BigInteger[] polynomial = new BigInteger[polyDegree + 1];
        polynomial[polyDegree] = secret;
        for (int i = 0; i < polyDegree; i++) {
            polynomial[i] = new BigInteger(field.bitLength() - 1, rndGenerator);
        }

        //calculating shares
        Share[] shares = new Share[nShareholders];
        for (int i = 0; i < nShareholders; i++) {
            BigInteger shareholder = BigInteger.valueOf(i + 1); //shareholder id can be any positive number, except 0
            BigInteger share = calculatePoint(shareholder, polynomial);
            shares[i] = new Share(user1,user2,shareholder, share);
        }

        return shares;
    }

    /**
     * This method combines shares, using Lagrange polynomials, to recover the secret.
     * 	Lagrange polynomials: https://en.wikipedia.org/wiki/Lagrange_polynomial.
     * @param shares Shares of the secret.
     * @return Recovered secret.
     */
    private static BigInteger combine(Share[] shares) {
        BigInteger recoveredSecret = BigInteger.ZERO;
        BigInteger x = BigInteger.ZERO;
        for (int j = 0; j < shares.length; j++) {
            BigInteger xj = shares[j].getShareholder();
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;
            for (int m = 0; m < shares.length; m++) {
                if (j == m) {
                    continue;
                }
                BigInteger xm = shares[m].getShareholder();
                numerator = numerator.multiply(x.subtract(xm));
                denominator = denominator.multiply(xj.subtract(xm));
            }
            BigInteger lj = numerator.multiply(denominator.modInverse(field));
            recoveredSecret = recoveredSecret.add(shares[j].getShare().multiply(lj)).mod(field);
        }
        return recoveredSecret;
    }

    /**
     * This method calculates a point on a polynomial using the Horner's method:
     * 	https://en.wikipedia.org/wiki/Horner%27s_method.
     * @param x X value.
     * @param polynomial Polynomial P(x).
     * @return Y value.
     */
    private static BigInteger calculatePoint(BigInteger x, BigInteger[] polynomial) {
        BigInteger b = polynomial[0];
        for (int i = 1; i < polynomial.length; i++) {
            b = polynomial[i].add(b.multiply(x)).mod(field);
        }
        return b;
    }

}
