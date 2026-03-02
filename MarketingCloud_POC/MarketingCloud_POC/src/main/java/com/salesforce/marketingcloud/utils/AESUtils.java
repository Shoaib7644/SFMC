package com.salesforce.marketingcloud.utils;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.salesforce.marketingcloud.managers.FileReaderManager;

public class AESUtils {

	/**
	 * Helper Method for Encryption/Decryption
	 */
	public static IvParameterSpec generateIv() {
		byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		return new IvParameterSpec(iv);
	}

	/**
	 * Helper Method for Encryption/Decryption
	 */
	public static SecretKeyFactory generateKeyFactory() {
		SecretKeyFactory secretKeyFact = null;
		try {
			secretKeyFact = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return secretKeyFact;
	}

	/**
	 * Helper Method for Encryption/Decryption
	 */
	public static KeySpec getKeySpec(char[] secretKeyArray, byte[] saltInBytes, int size, int bits) {

		return new PBEKeySpec(secretKeyArray, saltInBytes, size, bits);
	}

	/**
	 * Helper Method for Encryption/Decryption
	 */
	public static SecretKey getSecretKey(SecretKeyFactory factory, KeySpec spec) {
		SecretKey key = null;
		try {
			key = factory.generateSecret(spec);
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return key;
	}

	/**
	 * Method for Password Encryption
	 */
	public static String encrypt(String strToEncrypt) {
		try {
			IvParameterSpec ivspec = AESUtils.generateIv();
			SecretKeyFactory factory = AESUtils.generateKeyFactory();
			KeySpec spec = AESUtils.getKeySpec(
					FileReaderManager.getInstance().getConfigReader().getSecretKey().toCharArray(),
					FileReaderManager.getInstance().getConfigReader().getSalt().getBytes(), 65536, 256);
			SecretKey tmp = AESUtils.getSecretKey(factory, spec);
			SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}
		return null;
	}

	/**
	 * Method for Password Decryption
	 */
	public static String decrypt(String strToDecrypt) {
		try {
			IvParameterSpec ivspec = AESUtils.generateIv();
			SecretKeyFactory factory = AESUtils.generateKeyFactory();
			KeySpec spec = AESUtils.getKeySpec(
					FileReaderManager.getInstance().getConfigReader().getSecretKey().toCharArray(),
					FileReaderManager.getInstance().getConfigReader().getSalt().getBytes(), 65536, 256);
			SecretKey tmp = AESUtils.getSecretKey(factory, spec);
			SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
			return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
		} catch (Exception e) {
			System.out.println("Error while decrypting: " + e.toString());
		}
		return null;
	}

}
