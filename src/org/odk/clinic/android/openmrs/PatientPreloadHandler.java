package org.odk.clinic.android.openmrs;

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.utils.IPreloadHandler;


/**
 * Preload handler for patient known biodata.
 * 
 * @author danielkayiwa
 *
 */
public class PatientPreloadHandler implements IPreloadHandler{

	private Patient patient;
	
	
	public PatientPreloadHandler(Patient patient){
		this.patient = patient;
	}
	
	
	@Override
	public boolean handlePostProcess(TreeElement arg0, String arg1) {
		return false;
	}

	@Override
	public IAnswerData handlePreload(String param) {
		if("patientId".equalsIgnoreCase(param))
			return new IntegerData(patient.getPatientId());
		else if("familyName".equalsIgnoreCase(param))
			return new StringData(patient.getFamilyName());
		else if("middleName".equalsIgnoreCase(param))
			return new StringData(patient.getMiddleName());
		else if("givenName".equalsIgnoreCase(param))
			return new StringData(patient.getGivenName());
		else if("sex".equalsIgnoreCase(param) || "gender".equalsIgnoreCase(param))
			return new StringData(patient.getGender());
		else if("patientIdentifier".equalsIgnoreCase(param))
			return new StringData(patient.getIdentifier());
		else if("birthDate".equalsIgnoreCase(param))
			return new DateData(patient.getBirthdate());
		else if("name".equalsIgnoreCase(param))
			return new StringData(patient.getName());
		/*else if("birthDateEstimated".equalsIgnoreCase(param))
			return new StringData(patient.getBirthdate());*/
		
		return null;
	}

	@Override
	public String preloadHandled() {
		return "patient";
	}
}
