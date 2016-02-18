package com.zlebank.zplatform.member.service.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zlebank.zplatform.commons.utils.RSAUtils;
import com.zlebank.zplatform.member.bean.CoopInsti;
import com.zlebank.zplatform.member.bean.CoopInstiMK;
import com.zlebank.zplatform.member.bean.enums.EncryptAlgorithm;
import com.zlebank.zplatform.member.bean.enums.TerminalAccessType;
import com.zlebank.zplatform.member.dao.CoopInstiDAO;
import com.zlebank.zplatform.member.exception.AbstractCoopInstiException;
import com.zlebank.zplatform.member.exception.GenerateCoopInstiCodeException;
import com.zlebank.zplatform.member.exception.GenerateCoopInstiMKException;
import com.zlebank.zplatform.member.exception.InstiNameExistedException;
import com.zlebank.zplatform.member.exception.OpenCoopInstiBusiAcctException;
import com.zlebank.zplatform.member.exception.PrimaykeyGeneratedException;
import com.zlebank.zplatform.member.pojo.PojoCoopInsti;
import com.zlebank.zplatform.member.pojo.PojoInstiMK;
import com.zlebank.zplatform.member.service.CoopInstiService;
import com.zlebank.zplatform.member.service.MemberService;
import com.zlebank.zplatform.member.service.PrimayKeyService;

@Service
public class CoopInstiServiceImpl implements CoopInstiService {

	@Autowired
	private MemberService memberService;
	@Autowired
	private CoopInstiDAO coopInstiDAO;
	@Autowired
	private PrimayKeyService primayKeyService;

	private static final String COOPINST_PARA_TYPE = "COOPINSTIBIN";
	private static final String COOPINST_CODE_SEQ = "SEQ_COOP_INSTI_CODE";

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
	public CoopInstiMK getCoopInstiMK(String instiCode,
			TerminalAccessType terminalAccessType) {

		PojoInstiMK pojoInstiMK = coopInstiDAO.getMKByInstiCode(instiCode,
				terminalAccessType);
		if (pojoInstiMK == null) {
			return null;
		}
		CoopInstiMK coopInstiMK = new CoopInstiMK(pojoInstiMK.getCoopInsti()
				.getInstiCode());
		BeanUtils.copyProperties(pojoInstiMK, coopInstiMK, new String[] { "id",
				"coopInsti" });
		return coopInstiMK;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public String createCoopInsti(String instiName, long userId)
			throws AbstractCoopInstiException {

		/*
		 * check if instiName is repeat
		 */
		instiName = instiName.trim();
		if (coopInstiDAO.isNameExist(instiName)) {
			throw new InstiNameExistedException();
		}

		/*
		 * generate coop insti code
		 */
		String coopInstiCode;
		try {
			coopInstiCode = primayKeyService.getNexId(COOPINST_PARA_TYPE,
					COOPINST_CODE_SEQ);
		} catch (PrimaykeyGeneratedException pge) {
			GenerateCoopInstiCodeException geice = new GenerateCoopInstiCodeException();
			geice.initCause(pge);
			throw geice;
		}
		/*
		 * instance and init a pojos
		 */
		PojoCoopInsti pojoCoopInsti = new PojoCoopInsti();
		pojoCoopInsti.setInstiCode(coopInstiCode);
		pojoCoopInsti.setInstiName(instiName);
		pojoCoopInsti.setInUserId(userId);
		pojoCoopInsti.setInTime(new Timestamp(new Date().getTime()));
		pojoCoopInsti.setStatus("00");
		List<PojoInstiMK> instiMKs = new ArrayList<PojoInstiMK>();

		/*
		 * generate institution key
		 */
		try {

			for (TerminalAccessType terminalAccessType : TerminalAccessType
					.values()) {
				if (terminalAccessType == TerminalAccessType.UNKNOW) {
					continue;
				}
				Map<String, Object> coopInsti_keyMap = RSAUtils.genKeyPair();
				Map<String, Object> plath_keyMap = RSAUtils.genKeyPair();
				String coopInsti_publicKey = RSAUtils
						.getPublicKey(coopInsti_keyMap);
				String coopInsti_privateKey = RSAUtils
						.getPrivateKey(coopInsti_keyMap);
				String plath_publicKey = RSAUtils.getPublicKey(plath_keyMap);
				String plath_privateKey = RSAUtils.getPrivateKey(plath_keyMap);

				PojoInstiMK instiMK = new PojoInstiMK();
				instiMK.setCoopInsti(pojoCoopInsti);
				instiMK.setEncryptAlgorithm(EncryptAlgorithm.RSA);
				instiMK.setTerminalAccessType(terminalAccessType);
				instiMK.setInstiPriKey(coopInsti_privateKey);
				instiMK.setInstiPubKey(coopInsti_publicKey);
				instiMK.setZplatformPriKey(plath_privateKey);
				instiMK.setZplatformPubKey(plath_publicKey);
				instiMKs.add(instiMK);
			}
			pojoCoopInsti.setInstisMKs(instiMKs);
		} catch (Exception e) {
			e.printStackTrace();
			throw new GenerateCoopInstiMKException();
		}
		// open institution account
		try {
			memberService.openBusiAcct(instiName, coopInstiCode, userId);
		} catch (Exception e) {
			e.printStackTrace();
			OpenCoopInstiBusiAcctException cie = new OpenCoopInstiBusiAcctException(
					e.getMessage());
			throw cie;
		}

		// prestance to db
		coopInstiDAO.saveA(pojoCoopInsti);
		return pojoCoopInsti.getInstiCode();
	}

	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
	public List<CoopInsti> getAllCoopInsti() {
		List<PojoCoopInsti> pojoCoopInstis = coopInstiDAO.getCoopInstiList();
		List<CoopInsti> coopInstis = new ArrayList<CoopInsti>();
		String[] ignoreProperties = new String[] { "instisMKs" };
		for (PojoCoopInsti copyFrom : pojoCoopInstis) {
			CoopInsti copyTo = new CoopInsti();
			BeanUtils.copyProperties(copyFrom, copyTo, ignoreProperties);
			coopInstis.add(copyTo);
		}
		return coopInstis;
	}

	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
	public CoopInsti getInstiByInstiCode(String instiCode) {
		PojoCoopInsti pojoCoopInsit = null;
		try {
			pojoCoopInsit = coopInstiDAO.getByInstiCode(instiCode);
		} catch (HibernateException e) {
			e.printStackTrace();
			return null;
		}
		if (pojoCoopInsit == null) {
			return null;
		}
		CoopInsti copyTo = new CoopInsti();
		String[] ignoreProperties = new String[] { "instisMKs" };
		BeanUtils.copyProperties(pojoCoopInsit, copyTo, ignoreProperties);
		return copyTo;
	}
}
