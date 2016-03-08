/**
 * @author proshanto
 * */
package org.opensrp.register.mcare.service.scheduling;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static java.text.MessageFormat.format;
import static org.opensrp.dto.BeneficiaryType.elco;
import static org.opensrp.register.mcare.OpenSRPScheduleConstants.DateTimeDuration.duration;
import static org.opensrp.register.mcare.OpenSRPScheduleConstants.ELCOSchedulesConstants.ELCO_SCHEDULE_PSRF;
import static org.opensrp.register.mcare.OpenSRPScheduleConstants.ELCOSchedulesConstantsImediate.IMD_ELCO_SCHEDULE_PSRF;
import static org.opensrp.register.mcare.OpenSRPScheduleConstants.MotherScheduleConstants.SCHEDULE_ANC;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.motechproject.scheduletracking.api.domain.Enrollment;
import org.motechproject.scheduletracking.api.domain.MilestoneFulfillment;
import org.opensrp.common.AllConstants.ELCOSchedulesConstantsImediate;
import org.opensrp.connector.HttpUtil;
import org.opensrp.connector.openmrs.service.OpenmrsSchedulerService;
import org.opensrp.connector.openmrs.service.OpenmrsService;
import org.opensrp.connector.openmrs.service.OpenmrsUserService;
import org.opensrp.dto.ActionData;
import org.opensrp.dto.AlertStatus;
import org.opensrp.dto.BeneficiaryType;
import org.opensrp.form.domain.FormSubmission;
import org.opensrp.scheduler.Action;
import org.opensrp.scheduler.HealthSchedulerService;
import org.opensrp.scheduler.ScheduleLog;
import org.opensrp.scheduler.repository.AllActions;
import org.opensrp.scheduler.repository.AllReportActions;
import org.opensrp.scheduler.service.AllEnrollmentWrapper;
import org.opensrp.scheduler.service.ReportActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
public class ScheduleLogService extends OpenmrsService{
	private static final String TRACK_URL = "ws/rest/v1/scheduletracker/track";
	private static final String TRACK_MILESTONE_URL = "ws/rest/v1/scheduletracker/trackmilestone";
	private static final String SCHEDULE_URL = "ws/rest/v1/scheduletracker/schedule";
	private static final String MILESTONE_URL = "ws/rest/v1/scheduletracker/milestone";
	private static Logger logger = LoggerFactory.getLogger(ScheduleLogService.class.toString());
	private ReportActionService reportActionService;
	private final AllEnrollmentWrapper allEnrollments;
	private AllReportActions allReportActions;
	private AllActions allActions;
	private OpenmrsUserService userService;
	private HealthSchedulerService scheduler;
	
	@Autowired
	public ScheduleLogService(ReportActionService reportActionService,AllEnrollmentWrapper allEnrollments,AllReportActions allReportActions,AllActions allActions,OpenmrsUserService userService,HealthSchedulerService scheduler){
		this.reportActionService = reportActionService;
		this.allEnrollments = allEnrollments;
		this.allReportActions = allReportActions;
		this.allActions = allActions;
		this.userService = userService;
		this.scheduler = scheduler;
	}
	
	/**
	 * @author proshanto
	 * @desc This method save scheduleLog
	 * @param beneficiaryType Type of Beneficiary
	 * @param caseID  Beneficiary CaseId
	 * @param instanceId 
	 * @param anmIdentifier user name of field worker
	 * @param  scheduleName 
	 * @param alertStatus type of current window status
	 * @param visitCode current milestone name
	 * @param startDate Schedule start date
	 * @param expiryDate Schedule expired date
	 * 
	 * @return nothing to return
	 * 
	 * 
	 * */
	
	public void saveScheduleLog(BeneficiaryType beneficiaryType, String caseID, String instanceId, String anmIdentifier, String scheduleName, String visitCode, AlertStatus alertStatus, DateTime startDate, DateTime expiryDate,String immediateScheduleName,long timeStamp){
		List<Enrollment> el =this.findEnrollmentByCaseIdAndScheduleName(caseID,immediateScheduleName);
		String trackId = "";		
		for (Enrollment e : el){
			//trackId = this.saveEnrollDataToOpenMRSTrack(e);
		}		
		if(trackId.equalsIgnoreCase("")){
			trackId = "";
		}		
		try{
			reportActionService.alertForReporting(beneficiaryType, caseID, instanceId, anmIdentifier, scheduleName, visitCode, alertStatus, startDate, expiryDate,null,trackId,timeStamp);
			logger.info("ScheduleLog created with id case id :"+caseID);
		}catch(Exception e){
			logger.info("ScheduleLog Does not create:"+e.getMessage());
		}
		/*try {
			this.saveActionDataToOpenMrsMilestoneTrack(caseID, instanceId, anmIdentifier, scheduleName);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
	}
	
	public  List<Enrollment> findEnrollmentByCaseIdAndScheduleName(String caseID,String scheduleName ){
		return  allEnrollments.findByEnrollmentByExternalIdAndScheduleName(caseID,scheduleName);
	}
	
	public String saveEnrollDataToOpenMRSTrack(Enrollment e){
		JSONObject t = new JSONObject();
		String trackuuid = null;
		try {
			//t.put("beneficiary", e.getExternalId());
			t.put("beneficiary", 123456789);
			t.put("beneficiaryRole", "elco");
			t.put("schedule", e.getScheduleName().replace(ELCOSchedulesConstantsImediate.IMD_ELCO_SCHEDULE_PSRF,ELCO_SCHEDULE_PSRF));
			String hr = StringUtils.leftPad(e.getPreferredAlertTime().getHour().toString(),2,"0");
			String mn = StringUtils.leftPad(e.getPreferredAlertTime().getMinute().toString(),2,"0");
			t.put("preferredAlertTime", hr+":"+mn+":00");
			t.put("referenceDate", OPENMRS_DATE.format(e.getStartOfSchedule().toDate()));
			t.put("referenceDateType", "MANUAL");
			t.put("dateEnrolled", OPENMRS_DATE.format(e.getEnrolledOn().toDate()));			
			t.put("currentMilestone", e.getCurrentMilestoneName().replace(ELCOSchedulesConstantsImediate.IMD_ELCO_SCHEDULE_PSRF,ELCO_SCHEDULE_PSRF));
			t.put("status", e.getStatus().name());
			System.out.println("OpenMRS sent data:"+t.toString());
			JSONObject to = new JSONObject(HttpUtil.post(getURL()+"/"+TRACK_URL, "", t.toString(), OPENMRS_USER, OPENMRS_PWD).body());
			trackuuid = to.getString("uuid");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return trackuuid;
		
	}
	
	public void closeSchedule(String caseId,String instanceId,long timestamp,String name){
		        
        try{
        	ScheduleLog  schedule = allReportActions.findByTimestampIdByCaseIdByname(timestamp,caseId,name);
    		schedule.setRevision(schedule.getRevision());
            schedule.scheduleCloseDate(new DateTime());
            schedule.closeById(instanceId);
            schedule.setIsActionActive(false);
        	allReportActions.update(schedule);
        	logger.info("ScheduleLog close with id case id :"+caseId +" InstanceId: "+instanceId);
        }catch(Exception e){
        	logger.info("ScheduleLog Data not found.:"+e.getMessage());
        }
		
	}
	public void closeScheduleAndScheduleLog(String caseId,String instanceId,String scheduleName,String provider){
		try{
			List<Action> beforeNewActions = allActions.findAlertByANMIdEntityIdScheduleName(provider, caseId, scheduleName);
			if(beforeNewActions.size() > 0){ 
				this.closeSchedule(caseId,instanceId,beforeNewActions.get(0).timestamp(),scheduleName);
			}
			
		}catch(Exception e){
			 logger.info("From closeScheduleAndScheduleLog:"+e.getMessage());
		}
	}
	public void saveActionDataToOpenMrsMilestoneTrack(String entityId, String instanceId,
			String providerId, String schedule) throws ParseException{
		
		Enrollment e =allEnrollments.findByEnrollmentByExternalIdAndScheduleName(entityId, schedule).get(0) ;
		ScheduleLog  scheduleLog = allReportActions.findByInstanceIdByCaseIdByname(instanceId,entityId,schedule);
		List<Action> alertActions = allActions.findAlertByANMIdEntityIdScheduleName(providerId, entityId, schedule);
		Action close = getClosedAction(scheduleLog.getVisitCode(), alertActions);
		
		JSONObject tm = new JSONObject();
		try {
			tm.put("track", scheduleLog.trackId());
			MilestoneFulfillment m = getMilestone(scheduleLog.getVisitCode(), e);
			tm.put("milestone", scheduleLog.getVisitCode());
			JSONObject pr = userService.getPersonByUser(providerId);
			tm.put("alertRecipient", pr.getString("uuid"));
			tm.put("alertRecipientRole", "PROVIDER");
			String fdate = m == null?null:OPENMRS_DATE.format(m.getFulfillmentDateTime().toDate());
			if(fdate == null){
				fdate = close==null?null:OPENMRS_DATE.format(new SimpleDateFormat("dd-MM-yyyy").parse(close.data().get("completionDate")));
			}
			tm.put("fulfillmentDate", fdate);
			tm.put("status", scheduleLog.getCurrentWindow()+(close==null?"":"-completed"));
			//TODO tm.put("reasonClosed", ac.data().get(""));
			tm.put("alertStartDate", scheduleLog.getCurrentWindowStartDate());
			tm.put("alertExpiryDate", scheduleLog.getCurrentWindowEndDate());
			//tm.put("isActive", scheduleLog.get);
			tm.put("actionType", "PROVIDER ALERT");
			
			JSONObject tmo = new JSONObject(HttpUtil.post(getURL()+"/"+TRACK_MILESTONE_URL, "", tm.toString(), OPENMRS_USER, OPENMRS_PWD).body());
		
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
	private Action getClosedAction(String milestone, List<Action> actions){
		for (Action a : actions) {
			if(a.data().get("visitCode") != null && a.data().get("visitCode").equalsIgnoreCase(milestone)
					&& a.actionType().equalsIgnoreCase("closeAlert")){
				return a;
			}
		}
		return null;
	}
	private MilestoneFulfillment getMilestone(String milestone, Enrollment e) {
		for (MilestoneFulfillment m : e.getFulfillments()) {
			if(m.getMilestoneName().equalsIgnoreCase(milestone)){
				return m;
			}
		}
		return null;
	}
	
	public void fullfillSchedule(String caseID, String scheduleName, String instanceId,long timestamp){
		reportActionService.schedulefullfill(caseID, scheduleName, instanceId, timestamp);
		
	}
	
	public void createImmediateScheduleAndScheduleLog(String caseId, String date,String provider,String instanceId,BeneficiaryType beneficiaryType,String scheduleName,Integer durationInHour){
		try{
			allActions.addOrUpdateAlert(new Action(caseId, provider, ActionData.createAlert(beneficiaryType, scheduleName, scheduleName, AlertStatus.upcoming, new DateTime(), new DateTime().plusHours(durationInHour))));	    
			List<Action> existingAlerts = allActions.findAlertByANMIdEntityIdScheduleName(provider, caseId, scheduleName);
			if(existingAlerts.size() > 0){ 
				this.saveScheduleLog(beneficiaryType, caseId, instanceId, provider, scheduleName, scheduleName, AlertStatus.upcoming, new DateTime(), new DateTime().plusHours(durationInHour), "",existingAlerts.get(0).timestamp());
				logger.info("Create a Schedule Log with id :"+caseId);
			}
				
		}catch(Exception e){
		    logger.info("From createImmediateScheduleAndScheduleLog:"+e.getMessage());
		}		
	}
	
	public void createNewScheduleLogandUnenrollImmediateSchedule(String caseId, String date,String provider,String instanceId,String immediateScheduleName,String scheduleName,BeneficiaryType beneficiaryType,Integer durationInHour){
		try{
			scheduler.unEnrollFromScheduleimediate(caseId, provider, immediateScheduleName);
		}catch(Exception e){
			logger.info(format("Failed to UnEnrollFromSchedule PSRF"));
		}
		
		this.scheduleCloseAndSave(caseId, instanceId, provider, scheduleName, scheduleName, beneficiaryType, AlertStatus.normal, new DateTime(), new DateTime().plusHours(durationInHour));
		
	}
	public void scheduleCloseAndSave(String entityId,String instanceId,String provider,String ScheduleName,String milestoneName,BeneficiaryType beneficiaryType,AlertStatus alertStaus, DateTime startDate, DateTime expiredDate){
		try{
			List<Action> beforeNewActions = allActions.findAlertByANMIdEntityIdScheduleName(provider, entityId, ScheduleName);
			if(beforeNewActions.size() > 0){ 
			 this.closeSchedule(entityId,instanceId,beforeNewActions.get(0).timestamp(),ScheduleName);
			 logger.info("close a schedule with id : "+entityId);
			}
			
		}catch(Exception e){
			logger.info("From scheduleCloseAndSave for close : "+e.getMessage());
		}
		try{
			allActions.addOrUpdateAlert(new Action(entityId, provider, ActionData.createAlert(beneficiaryType, ScheduleName, milestoneName, alertStaus, startDate,  expiredDate)));
			logger.info(format("create psrf from psrf to psrf..", entityId));
			List<Action> afterNewActions = allActions.findAlertByANMIdEntityIdScheduleName(provider, entityId, ScheduleName);
			if(afterNewActions.size() > 0){ 
				this.saveScheduleLog(beneficiaryType, entityId, instanceId, provider, ScheduleName, milestoneName, alertStaus, startDate, expiredDate,ScheduleName,afterNewActions.get(0).timestamp());
				logger.info("create a schedule with id : "+entityId);
			}
			
		}catch(Exception e){
			logger.info("From scheduleCloseAndSave for save: "+e.getMessage());
		}
	}
}