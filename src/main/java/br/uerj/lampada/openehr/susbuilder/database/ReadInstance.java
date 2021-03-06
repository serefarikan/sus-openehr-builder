package br.uerj.lampada.openehr.susbuilder.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import br.uerj.lampada.openehr.susbuilder.mapping.Mapping;
import br.uerj.lampada.openehr.susbuilder.utils.Constants;

/*
 * Class used to create queries for each template
 * and retrieve instances from the database 
 * 
 */
public class ReadInstance {

	public static final String AIH_PATIENT_ID = "n_aih";

	public static final String APAC_PATIENT_ID = "ap_cnspcn";
	public static final String PATIENT_ID_COL = "patient_id";
	private static Logger log = Logger.getLogger(ReadInstance.class);
	private DBHandler db;
	private boolean isAIH;
	private ResultSet rs;

	private String template;

	public ReadInstance(boolean isAIH, DBHandler db) {
		this(isAIH, null, db);
	}

	public ReadInstance(boolean isAIH, String template, DBHandler db) {
		this.template = template;
		this.isAIH = isAIH;
		this.db = db;
	}

	/*
	 * Retrieve values from a given column in a template (Used only in tests)
	 */
	public void column(String template, String column) throws Exception {
		column(template, column, null);
	}

	public void column(String template, String column, Integer limit)
			throws Exception {
		rs = null;

		String sql = null;
		String table = getTableNameFromTemplate(template);

		sql = "select distinct " + column + " from " + table;
		if (limit != null) {
			sql += " limit " + limit;
		}

		db.setSql(sql);
		rs = db.runQuery();
	}

	/*
	 * Get patient id list
	 */
	public void patientId(boolean isAIH) throws Exception {
		patientId(isAIH, null);
	}

	public void patientId(boolean isAIH, Integer limit) throws Exception {
		rs = null;

		List<String> tables = new ArrayList<String>();
		String pIdColumn = APAC_PATIENT_ID;
		List<String> templates = Constants.templatesAPAC;
		if (isAIH) {
			pIdColumn = AIH_PATIENT_ID;
			templates = Constants.templatesAIH;
		}

		for (String temp : templates) {
			if (!temp.equals(Constants.DEMOGRAPHIC_DATA)) {
				tables.add(getTableNameFromTemplate(temp));
			}
		}

		if (tables.size() == 0) {
			throw new Exception("Cannot use these templates");
		}

		String sql = "select " + PATIENT_ID_COL + " from ( ";
		for (int i = 0; i < tables.size(); i++) {
			sql += "select " + pIdColumn + " as " + PATIENT_ID_COL + " from "
					+ tables.get(i);
			if (i < tables.size() - 1) {
				sql += " union all ";
			}
		}
		sql += " ) as patient_table where " + PATIENT_ID_COL + " is not null "
				+ " group by " + PATIENT_ID_COL;
		if (limit != null) {
			sql += " limit " + limit;
		}

		rs = db.runQuery(sql);
	}

	// /*
	// * Get terminology label for code
	// */
	// public void termLabel(String term, String code) throws Exception {
	// rs = null;
	// stTerminology.setString(1, term);
	// stTerminology.setString(2, code);
	// db.setStatement(stTerminology);
	// db.setSql(queryTerminology());
	// rs = db.runQuery();
	// }

	/*
	 * Run query for patient id
	 */
	public void queryTemplate(String patientId) throws Exception {
		rs = null;
		if (template.equals(Constants.BARIATRIC_SURGERY)) {
			db.setSql(queryBariatrics(patientId));
		} else if (template.equals(Constants.CHEMOTHERAPY)) {
			db.setSql(queryChemotherapy(patientId));
		} else if (template.equals(Constants.DEMOGRAPHIC_DATA)) {
			db.setSql(queryDemographic(patientId));
		} else if (template.equals(Constants.HOSPITALISATION)) {
			db.setSql(queryHospitalisation(patientId));
		} else if (template.equals(Constants.MEDICATION)) {
			db.setSql(queryMedication(patientId));
		} else if (template.equals(Constants.MISCELLANEOUS)) {
			db.setSql(queryMiscellaneous(patientId));
		} else if (template.equals(Constants.NEPHROLOGY)) {
			db.setSql(queryNephrology(patientId));
		} else if (template.equals(Constants.RADIOTHERAPY)) {
			db.setSql(queryRadiotherapy(patientId));
		} else {
			throw new Exception("Unknown template");
		}

		rs = db.runQuery();
	}

	/*
	 * Retrieve results to list of hash
	 */
	public List<HashMap<String, Object>> retrieveResultsToLoH() {
		List<HashMap<String, Object>> result = new ArrayList<HashMap<String, Object>>();
		ResultSetMetaData md;

		if (rs == null) {
			return result;
		}

		try {
			md = rs.getMetaData();
			int cols = md.getColumnCount();
			Map<String, String> columnToPath = new HashMap<String, String>();
			for (int i = 1; i < cols + 1; i++) {
				String label = md.getColumnName(i);
				String path = Mapping.pathFromColumn(label, template);
				if (path != null) {
					columnToPath.put(label, path);
				} else {
					columnToPath.put(label, label);
				}
			}

			while (rs.next()) {
				HashMap<String, Object> rItem = new HashMap<String, Object>();
				for (int i = 1; i < cols + 1; i++) {
					String label = md.getColumnName(i);
					label = columnToPath.get(label);
					if (Mapping.getColumnMap(template).get(label) instanceof List) {
						List<String> res = new ArrayList<String>();
						if (rItem.containsKey(label)) {
							res = (List<String>) (rItem.get(label));
						}
						res.add(rs.getObject(i).toString().trim());
						rItem.put(label, res);
					} else {
						rItem.put(label, rs.getObject(i).toString().trim());
					}
				}
				result.add(rItem);
			}
		} catch (SQLException e) {
			log.error(e.getMessage());
		}
		return result;
	}

	/*
	 * Retrieve results to list of list
	 */
	public List<ArrayList<String>> retrieveResultsToLoL() { // List of List
		List<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		ResultSetMetaData md;

		if (rs == null) {
			return result;
		}

		try {
			md = rs.getMetaData();
			int cols = md.getColumnCount();
			while (rs.next()) {
				ArrayList<String> rItem = new ArrayList<String>();
				for (int i = 0; i < cols; i++) {
					rItem.add(rs.getObject(i).toString().trim());
				}
				result.add(rItem);
			}
		} catch (SQLException e) {
			log.error(e.getMessage());
		}
		return result;
	}

	/*
	 * Retrieve single column result to hash without mapping column name to path
	 */
	public HashMap<String, String> retrieveSingleResultToHash() {
		return retrieveSingleResultToHash(null);
	}

	/*
	 * Retrieve single column result to hash mapping automatically column name
	 * to path
	 */
	public HashMap<String, String> retrieveSingleResultToHash(
			Map<String, String> pathMap) {
		HashMap<String, String> result = new HashMap<String, String>();
		ResultSetMetaData md;
		if (rs == null) {
			return result;
		}
		try {
			md = rs.getMetaData();
			int cols = md.getColumnCount();
			while (rs.next()) {
				for (int i = 1; i < cols + 1; i++) {
					String label = md.getColumnName(i);
					if (pathMap != null)
						label = pathMap.get(label);
					result.put(label, rs.getObject(i).toString().trim());
				}
			}
		} catch (SQLException e) {
			log.error(e.getMessage());
		}
		return result;
	}

	/*
	 * Retrieve single column result to list
	 */
	public List<String> retrieveSingleResultToList() {
		List<String> result = new ArrayList<String>();
		if (rs == null) {
			return result;
		}
		try {
			while (rs.next()) {
				result.add(rs.getObject(1).toString().trim());
			}
		} catch (SQLException e) {
			log.error(e.getMessage());
		}
		return result;
	}

	/*
	 * Table name mapping for template (Could be Map)
	 */
	private String getTableNameFromTemplate(String template) {
		String table = null;
		if (template.equals(Constants.BARIATRIC_SURGERY)) {
			table = "bariatrics";
		} else if (template.equals(Constants.CHEMOTHERAPY)) {
			table = "chemotherapy";
		} else if (template.equals(Constants.DEMOGRAPHIC_DATA)) {
			table = "";
		} else if (template.equals(Constants.HOSPITALISATION)) {
			table = "hospitalisation";
		} else if (template.equals(Constants.MEDICATION)) {
			table = "medication";
		} else if (template.equals(Constants.NEPHROLOGY)) {
			table = "nephrology";
		} else if (template.equals(Constants.MISCELLANEOUS)) {
			table = "miscellaneous";
		} else if (template.equals(Constants.RADIOTHERAPY)) {
			table = "radiotherapy";
			// } else {
			// throw new Exception("Unknow table for template "+template);
		}
		return table;
	}

	/*
	 * Create bariatrics query
	 */
	private String queryBariatrics(String patientId) {

		String sql = "select ap_cmp, " // issue date
				+ "ap_coduni, " // healthcare unit
				+ "estado, " // state
				+ "ap_nuidade, " // age
				+ "ap_tpaten, " // reason for encounter
				+ "ap_motsai, "// reason for discharge
				+ "ap_dtocor, " // date of discharge
				+ "ap_cidpri, " // main diagnosis
				+ "ap_cidsec, " // secondary diagnosis
				+ "ap_cidcas, "// associated causes
				+ "ap_pripal, " // performed procedure

				+ "ab_imc, " // body mass index
				+ "ab_dtcirur, " // date of procedure
				+ "ab_mesacom, " // follow-up in months
				+ "ab_pontbar, " // Baros score
				+ "ab_tabbarr " // Baros table

				+ "from "
				+ getTableNameFromTemplate(Constants.BARIATRIC_SURGERY);
		sql += " where " + APAC_PATIENT_ID + " = '" + patientId + "'";
		return sql;
	}

	/*
	 * Create chemotherapy query
	 */
	private String queryChemotherapy(String patientId) {

		String sql = "select ap_cmp, " // issue date
				+ "ap_coduni, " // healthcare unit
				+ "estado, " // state
				+ "ap_nuidade, " // age
				+ "ap_tpaten, " // reason for encounter
				+ "ap_motsai, " // reason for discharge
				+ "ap_dtocor, " // date of discharge
				+ "ap_cidpri, " // main diagnosis
				+ "ap_cidsec, " // secondary diagnosis
				+ "ap_cidcas, " // associated causes
				+ "ap_pripal, " // performed procedure

				+ "aq_cid10, " // topography
				+ "aq_linfin, " // regional linphonodes
				+ "aq_estadi, " // tumour stage
				+ "aq_grahis, " // histopathological grading
				+ "aq_dtiden, " // date of pathological identification
				+ "aq_dtintr, " // date of begin of treatment
				+ "trim(aq_esqu_p1) || trim(aq_esqu_p2) as aq_esqu, " // schema
				+ "aq_totmpl " // duration of treatment (months)

				+ "from " + getTableNameFromTemplate(Constants.CHEMOTHERAPY);
		sql += " where " + APAC_PATIENT_ID + " = '" + patientId + "'";
		return sql;
	}

	/*
	 * Create demographic query
	 */
	private String queryDemographic(String patientId) {

		List<String> tables = new ArrayList<String>();
		String columns = null;
		String pIdColumn = null;
		String fcolumns = "ap_etnia, ap_racacor, ap_sexo, ap_ufnacio, nasc, instru";

		if (isAIH) {
			pIdColumn = AIH_PATIENT_ID;
			columns = "'' ap_etnia, '' ap_racacor, sexo ap_sexo, nacional ap_ufnacio, nasc, instru";
			tables.add(getTableNameFromTemplate(Constants.HOSPITALISATION));
		} else {
			pIdColumn = APAC_PATIENT_ID;
			columns = "ap_sexo, ap_racacor, ap_etnia, ap_ufnacio, '' nasc, '' instru";
			for (String temp : Constants.templatesAPAC) {
				if (!temp.equals(Constants.DEMOGRAPHIC_DATA)) {
					tables.add(getTableNameFromTemplate(temp));
				}
			}
		}

		String sql = "select " + fcolumns + " from ( ";
		for (int i = 0; i < tables.size(); i++) {
			sql += " select " + columns + " from " + tables.get(i) + " where "
					+ pIdColumn + " = '" + patientId + "'";
			if (i < tables.size() - 1) {
				sql += " union all ";
			}
		}
		sql += " ) as demographic limit 1";

		return sql;
	}

	/*
	 * Create hospitalisation query
	 */
	private String queryHospitalisation(String patientId) {

		String sql = "select dt_inter, " // admission date
				+ "car_int, " // type of hospitalisation
				+ "espec, " // speciality
				+ "cgc_hosp, " // healthcare unit
				+ "upper(estado) as estado, " // state
				+ "dt_saida, " // discharge date
				+ "cobranca, " // claim reason
				+ "morte, " // death indicator
				+ "ano_cmpt || mes_cmpt as dt_cmpt, " // claim issue date
				+ "uti_mes_to, " // ICU – total stay
				+ "infehosp, " // hospitalisation infection
				+ "proc_rea, " // performed procedure
				+ "diag_princ, " // main diagnosis
				+ "diag_secun " // secondary diagnosis

				+ "from " + getTableNameFromTemplate(Constants.HOSPITALISATION);
		sql += " where " + AIH_PATIENT_ID + " = '" + patientId + "'";
		return sql;
	}

	/*
	 * Create medication query
	 */
	private String queryMedication(String patientId) {
		String sql = "select ap_cmp, " // issue date
				+ "ap_coduni, " // healthcare unit
				+ "estado, " // state
				+ "ap_nuidade, " // age
				+ "ap_tpaten, " // reason for encounter
				+ "ap_motsai, " // reason for discharge
				+ "ap_dtocor, " // date of discharge
				+ "ap_cidpri, " // main diagnosis
				+ "ap_cidsec, " // secondary diagnosis
				+ "ap_cidcas, " // associated causes
				+ "ap_pripal, " // performed procedure

				+ "am_peso, " // weight
				+ "am_altura, " // height
				+ "am_transpl, " // indicator of transplantation
				+ "am_qtdtran " // number of transplantations
				+ "from " + getTableNameFromTemplate(Constants.MEDICATION);
		sql += " where " + APAC_PATIENT_ID + " = '" + patientId + "'";
		return sql;
	}

	/*
	 * Create outpatient various query
	 */
	private String queryMiscellaneous(String patientId) {

		String sql = "select ap_cmp, " // issue date
				+ "ap_coduni, " // healthcare unit
				+ "estado, " // state
				+ "ap_nuidade, " // age
				+ "ap_tpaten, " // reason for encounter
				+ "ap_motsai, " // reason for discharge
				+ "ap_dtocor, " // date of discharge
				+ "ap_cidpri, " // main diagnosis
				+ "ap_cidsec, " // secondary diagnosis
				+ "ap_cidcas, " // associated causes
				+ "ap_pripal " // performed procedure

				+ "from " + getTableNameFromTemplate(Constants.MISCELLANEOUS);
		sql += " where " + APAC_PATIENT_ID + " = '" + patientId + "'";
		return sql;
	}

	/*
	 * Create nephrology query
	 */
	private String queryNephrology(String patientId) {

		String sql = "select ap_cmp, " // issue date
				+ "ap_coduni, " // healthcare unit
				+ "estado, " // state
				+ "ap_nuidade, " // age
				+ "ap_tpaten, " // reason for encounter
				+ "ap_motsai, " // reason for discharge
				+ "ap_dtocor, " // date of discharge
				+ "ap_cidpri, " // main diagnosis
				+ "ap_cidsec, " // secondary diagnosis
				+ "ap_pripal, " // performed procedure

				+ "an_dtpdr, " // date of first dialysis
				+ "an_peso, " // weight
				+ "an_altura, " // height
				+ "an_diures, " // diuresis
				+ "'urine' as an_urine, " // urine
				+ "an_glicos, " // glucose
				+ "an_acevas, " // vascular access
				+ "an_ulsoab, " // abdominal ultrasonography
				+ "an_tru, " // ureia reduction rate
				+ "an_intfis, " // venous fistula amount
				+ "an_cncdo, " // enrolled for transplantation
				+ "an_albumi, " // albumine
				+ "an_hcv, " // HIC - antibodies
				+ "an_hbsag, " // HbsAg
				+ "an_hiv, " // HIV
				+ "an_hb " // HB

				+ "from " + getTableNameFromTemplate(Constants.NEPHROLOGY);
		sql += " where " + APAC_PATIENT_ID + " = '" + patientId + "'";
		return sql;
	}

	/*
	 * Create radiotherapy query
	 */
	private String queryRadiotherapy(String patientId) {
		String sql = "select ap_cmp, " // issue date
				+ "ap_coduni, " // healthcare unit
				+ "estado, " // state
				+ "ap_nuidade, " // age
				+ "ap_tpaten, " // reason for encounter
				+ "ap_motsai, " // reason for discharge
				+ "ap_dtocor, " // date of discharge
				+ "ap_cidpri, " // main diagnosis
				+ "ap_cidsec, " // secondary diagnosis
				+ "ap_cidcas, " // associated causes
				+ "ap_pripal, " // performed procedure

				+ "ar_finali, " // reason for treatment
				+ "ar_cid10, " // topography
				+ "ar_linfin, " // regional linphonodes
				+ "ar_estadi, " // tumour stage
				+ "ar_grahis, " // histopathological grading
				+ "ar_dtiden, " // date of pathological identification
				+ "ar_dtintr, " // date of begin of treatment
				+ "ar_cidtr1, " // irradiated area 1
				+ "ar_cidtr2, " // irradiated area 2
				+ "ar_cidtr3, " // irradiated area 3
				+ "ar_numc1 " // number of fields/insertions
				+ "ar_numc2, " // number of fields/insertions
				+ "ar_numc3 " // number of fields/insertions

				+ "from " + getTableNameFromTemplate(Constants.RADIOTHERAPY);
		sql += " where " + APAC_PATIENT_ID + "= '" + patientId + "'";
		return sql;
	}
}
