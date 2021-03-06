package com.mpc.vrekon.service.impl;

import java.io.File;
import java.io.ObjectInputStream.GetField;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.record.PageBreakRecord.Break;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpc.vrekon.model.DBCopyStartRequest;
import com.mpc.vrekon.model.DBRequest;
import com.mpc.vrekon.model.DBResponse;
import com.mpc.vrekon.model.DBSetting;
import com.mpc.vrekon.model.DBTranslate;
import com.mpc.vrekon.model.Institusi1;
import com.mpc.vrekon.model.Institusi2;
import com.mpc.vrekon.model.Institusi3;
import com.mpc.vrekon.model.Institusi4;
import com.mpc.vrekon.model.Institusi5;
import com.mpc.vrekon.model.Institusi6;
import com.mpc.vrekon.model.InstitusiList;
import com.mpc.vrekon.model.SetupServiceResponse;
import com.mpc.vrekon.repository.DBSettingRepository;
import com.mpc.vrekon.repository.DBTranslateRepository;
import com.mpc.vrekon.repository.Institusi1Repository;
import com.mpc.vrekon.repository.Institusi2Repository;
import com.mpc.vrekon.repository.Institusi3Repository;
import com.mpc.vrekon.repository.Institusi4Repository;
import com.mpc.vrekon.repository.Institusi5Repository;
import com.mpc.vrekon.repository.Institusi6Repository;
import com.mpc.vrekon.repository.InstitusiListRepository;
import com.mpc.vrekon.service.SetupService;
import com.mpc.vrekon.util.ConnectionHelper;
import com.mpc.vrekon.util.GlobalHelper;
import com.mpc.vrekon.util.ReaderHelper;
import com.mpc.vrekon.util.ResponseHelper;
import com.mpc.vrekon.util.UtilHelper;

@Service
public class SetupServiceImpl implements SetupService{
	@Autowired ServletContext servletContext;
	@Autowired DBSettingRepository dbSettingRepository;
	@Autowired Institusi1Repository institusi1Repository;
	@Autowired Institusi2Repository institusi2Repository;
	@Autowired Institusi3Repository institusi3Repository;
	@Autowired Institusi4Repository institusi4Repository;
	@Autowired Institusi5Repository institusi5Repository;
	@Autowired Institusi6Repository institusi6Repository;
	@Autowired DBTranslateRepository dbTranslateRepository;
	@Autowired InstitusiListRepository institusiListRepository;
	@PersistenceContext EntityManager entityManager;
	
	ResponseHelper responseHelper = new ResponseHelper();
	GlobalHelper globalHelper = new GlobalHelper();
	UtilHelper utilHelper = new UtilHelper();
	Logger log = Logger.getLogger(getClass());
	
	public Map<String, List<?>> getSetup(HttpServletRequest request){
		List<InstitusiList> institusiList = new ArrayList<InstitusiList>();
		List<SetupServiceResponse> setupServiceList = new ArrayList<SetupServiceResponse>();
		
		try {
			institusiList = institusiListRepository.findAll();
			
			for (InstitusiList value : institusiList) {
				SetupServiceResponse setupServiceResponse = new SetupServiceResponse();
				List<DBSetting> dbSettings = new ArrayList<DBSetting>();
				
				for (DBSetting dbSetting : dbSettingRepository.findByIdInstitusi(value.getId())) {
					String sql = "select count(*) as count from institusi"+value.getInstitusiTabel()+" where id_service="+dbSetting.getId();
					ResultSet result = globalHelper.executeQuery(sql);
					result.next();
					
					//log.debug(result.getString("count"));
					if(Integer.parseInt(result.getString("count")) == 0){
						dbSetting.setStatus("Empty");
					}else{
						dbSetting.setStatus("Ready");
					}
					log.debug(dbSetting);
					dbSettings.add(dbSetting);
					globalHelper.closeConnection();
				}
				
				setupServiceResponse.setInstitusiTabel(value.getInstitusiTabel());
				setupServiceResponse.setName(value.getName());
				setupServiceResponse.setId(value.getId());
				
				setupServiceResponse.setInstitusiService(dbSettings);
				
				setupServiceList.add(setupServiceResponse);
			}
			
			responseHelper.putData(setupServiceList, "Success", "");
		} catch (Exception e) {
			responseHelper.putData(setupServiceList, "Failed",e.getMessage());
			e.printStackTrace();
		}
		
		return responseHelper.getResponseData();
	}
	
	public Map<String, List<?>> searchInstitusi(Integer idInstitusi){
		try {
			InstitusiList institusiList = new InstitusiList();
			institusiList = institusiListRepository.findOne(idInstitusi);
			
			if(institusiList == null){
				responseHelper.putData(new ArrayList<Object>(), "Failed","idInstitusi not found.");
			}else{
				responseHelper.putData(institusiList, "Success", "");
			}
		} catch (Exception e) {
			responseHelper.putData(new ArrayList<Object>(), "Failed",e.getMessage());
		}
		
		return responseHelper.getResponseData();
	}
	
	public Map<String, List<?>> addInstitusi(HttpServletRequest request, InstitusiList institusiList){
		try {
			Boolean error = false;
			int countInstitusiList = 0;
			
			if (institusiList.getInstitusiTabel() > 6){
				responseHelper.putData(new ArrayList<Object>(), "Failed", "Only 6 tables available, please check institusiTabel value.");
				error = true;
			}
			
			countInstitusiList = institusiListRepository.countByInstitusiTabel(institusiList.getInstitusiTabel());
			
			log.debug("Check Table Use: "+( countInstitusiList != 0 ));
			if(countInstitusiList != 0){
				responseHelper.putData(new ArrayList<Object>(), "Failed", "Table Institusi"+institusiList.getInstitusiTabel()+" already use by other Institusi");
				error = true;
			}
			
			log.debug("Error Status: "+error);
			if (!error) {
				InstitusiList tmpInstitusiList = new InstitusiList();
						
				tmpInstitusiList = institusiListRepository.save(institusiList);
				responseHelper.putData(tmpInstitusiList, "Success", "");
			}
		} catch (Exception e) {
			responseHelper.putData(new ArrayList<Object>(), "Failed", e.getMessage());
			log.debug(e.getMessage());
		}
		
		return responseHelper.getResponseData();
	}
	
	public Map<String, List<?>> editInstitusi(HttpServletRequest request, InstitusiList institusiList){
		try {
			Boolean error = false;
			int countInstitusiList = 0;
			
			if (institusiList.getInstitusiTabel() > 6){
				responseHelper.putData(new ArrayList<Object>(), "Failed", "Only 6 tables available, please check institusiTabel value.");
				error = true;
			}
			
			InstitusiList beforeInstitusi = new InstitusiList();
			beforeInstitusi = institusiListRepository.findOne(institusiList.getId());
			
			if(beforeInstitusi == null){
				responseHelper.putData(new ArrayList<Object>(), "Failed", "Id not found in Institusi table.");
				error = true;
			}else{
				countInstitusiList = institusiListRepository.countByInstitusiTabelAndIdNot(institusiList.getInstitusiTabel(), institusiList.getId());				
			}
			
			if(countInstitusiList != 0){
				responseHelper.putData(new ArrayList<Object>(), "Failed", "Table Institusi"+institusiList.getInstitusiTabel()+" already use by other Institusi");
				error = true;
			}
			
			if (!error) {
				InstitusiList tmpInstitusiList = new InstitusiList();
				String sql = "update institusi_list set institusi_tabel = "+institusiList.getInstitusiTabel()+", name = '"+institusiList.getName()+"' where id ="+institusiList.getId();
				globalHelper.updateQuery(sql);
				globalHelper.closeConnection();
				
				tmpInstitusiList = institusiListRepository.findOne(institusiList.getId());
				responseHelper.putData(tmpInstitusiList, "Success", "");
			}
		} catch (Exception e) {
			 responseHelper.putData(new ArrayList<Object>(), "Failed", e.getMessage());
		}
		return responseHelper.getResponseData();
	}
	
	public Map<String, List<?>> deleteInstitusi(HttpServletRequest request, Integer idInstitusi){		
		try {
			List<DBSetting> dbSettings = new ArrayList<DBSetting>();
			InstitusiList institusiList = new InstitusiList();
			
			institusiList = institusiListRepository.findOne(idInstitusi);
			
			if(institusiList == null){
				responseHelper.putData(new ArrayList<Object>(), "Failed", "Id not found in institusi table.");
			}else{
				dbSettings = dbSettingRepository.findByIdInstitusi(idInstitusi);
				
				for (DBSetting dbSetting : dbSettings) {
					String sql = "delete from dbtranslate where id_service = "+dbSetting.getId();
					globalHelper.updateQuery(sql);
					globalHelper.closeConnection();
				}
				
				String sql = "delete from dbsetting where id_institusi = "+idInstitusi;
				globalHelper.updateQuery(sql);
				globalHelper.closeConnection();
				
				sql = "delete from institusi_list where id = "+idInstitusi;
				globalHelper.updateQuery(sql);
				globalHelper.closeConnection();
				
				responseHelper.putData(new ArrayList<Object>(), "Success", "");				
			}
		} catch (Exception e) {
			responseHelper.putData(new ArrayList<Object>(), "Failed", e.getMessage());
			log.debug(e.getMessage());
		}
		
		return responseHelper.getResponseData();
	}
	
	public Map<String, List<?>> getDBSettingById(DBSetting dbSetting){
		try {
			DBResponse dbResponse = new DBResponse();
			DBSetting dbSettingTmp = dbSettingRepository.findOne(dbSetting.getId());
			
			if(dbSettingTmp == null){
				responseHelper.putData(new ArrayList<Object>(), "Failed", "Id not found in table dbsetiing.");
			}else{
				List<DBTranslate> dbTranslates = new ArrayList<DBTranslate>();
				
				dbTranslates = dbTranslateRepository.findByIdService(dbSettingTmp.getId());
				dbResponse.setDbHost(dbSettingTmp.getDbHost());
				dbResponse.setDbName(dbSettingTmp.getDbName());
				dbResponse.setDbPassword(dbSettingTmp.getDbPassword());
				dbResponse.setDbTableName(dbSettingTmp.getDbTableName());
				dbResponse.setDbType(dbSettingTmp.getDbType());
				dbResponse.setDbUsername(dbSettingTmp.getDbUsername());
				dbResponse.setStatus(dbSettingTmp.getStatus());
				dbResponse.setIdInstitusi(dbSettingTmp.getIdInstitusi());
				dbResponse.setId(dbSettingTmp.getId());
				dbResponse.setDbTranslates(dbTranslates);
				
				responseHelper.putData(dbResponse, "Success", "");				
			}
		} catch (Exception e) {
			responseHelper.putData(new ArrayList<Object>(), "Failed", e.getMessage());
		}
		return responseHelper.getResponseData();
	}
	
	public Map<String, List<?>> getDBSettingByIdInstitusi(InstitusiList institusiList){
		try {
			List<DBResponse> dbResponses = new ArrayList<DBResponse>();
			List<DBSetting> dbSettings = dbSettingRepository.findByIdInstitusi(institusiList.getId());
			
			if(dbSettings == null){
				responseHelper.putData(new ArrayList<Object>(), "Failed", "Id not found in table dbsetiing.");
			}else{				
				for (DBSetting dbSetting : dbSettings) {
					List<DBTranslate> dbTranslates = new ArrayList<DBTranslate>();
					DBResponse dbResponse = new DBResponse();
					dbTranslates = dbTranslateRepository.findByIdService(dbSetting.getId());
					
					dbResponse.setDbHost(dbSetting.getDbHost());
					dbResponse.setDbName(dbSetting.getDbName());
					dbResponse.setDbPassword(dbSetting.getDbPassword());
					dbResponse.setDbTableName(dbSetting.getDbTableName());
					dbResponse.setDbType(dbSetting.getDbType());
					dbResponse.setDbUsername(dbSetting.getDbUsername());
					dbResponse.setStatus(dbSetting.getStatus());
					dbResponse.setIdInstitusi(dbSetting.getIdInstitusi());
					dbResponse.setId(dbSetting.getId());
					dbResponse.setDbTranslates(dbTranslates);
					
					dbResponses.add(dbResponse);
				}
				
				responseHelper.putData(dbResponses, "Success", "");				
			}
		} catch (Exception e) {
			responseHelper.putData(new ArrayList<Object>(), "Failed", e.getMessage());
		}
		return responseHelper.getResponseData();
	}
	
	public Map<String, List<?>> addDBSetting(HttpServletRequest request, String json, MultipartFile[] files, HttpSession httpSession){
		try {
			List<DBTranslate> dbTranslates = new ArrayList<DBTranslate>();
			DBRequest dbRequest = new ObjectMapper().readValue(json, DBRequest.class);
			DBSetting dbSetting = dbRequest.getDbSetting();
			Boolean error = false;
			
			log.debug("Check data in database");
			if(institusiListRepository.findOne(dbSetting.getIdInstitusi()) == null){
				responseHelper.putData(new ArrayList<Object>(), "Failed", "idInstitusi not found in table institusiList, please check your input again.");
				error = true;
			}
			
			if(!error){
				dbSetting = dbSettingRepository.save(dbSetting);
				String allFileName = "";
				
				log.debug("Start to save data");
				for (DBTranslate dbTranslate : dbRequest.getDbTranslates()) {
					DBTranslate dbTranslateTmp = null;
					
					dbTranslate.setIdService(dbSetting.getId());
					dbTranslateTmp = dbTranslateRepository.save(dbTranslate);
					dbTranslates.add(dbTranslateTmp);
				}
				log.debug("Done to save data");
				
				if(files != null){
					log.debug("Start to save file");
					Integer i = 1;
					for(MultipartFile file : files){
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");  
						Date date = new Date();
						String fileName = dbSetting.getId()+"_"+dateFormat.format(date)+"_"+i+"."+globalHelper.getFileExtension(file.getOriginalFilename());
						if(globalHelper.getFileExtension(file.getOriginalFilename()).equals("xlsx") || globalHelper.getFileExtension(file.getOriginalFilename()).equals("xls")){
							File fileTransfer = new File(httpSession.getServletContext().getRealPath("/uploadFile/excel/"), fileName);
							file.transferTo(fileTransfer);
						}else if(globalHelper.getFileExtension(file.getOriginalFilename()).equals("txt")){
							File fileTransfer = new File(httpSession.getServletContext().getRealPath("/uploadFile/text/"), fileName);
							file.transferTo(fileTransfer);
						}
						allFileName += fileName+"%";
						i++;
					}
					allFileName += ";";
					allFileName = allFileName.replace("%;", "");
					
					log.debug("Done to save file");
				}
				
				String sql = "update dbsetting set files = '"+allFileName+"', status = 'Empty' where id = "+dbSetting.getId();
				globalHelper.updateQuery(sql);
				globalHelper.closeConnection();
				
				dbRequest.setDbSetting(dbSettingRepository.findOne(dbSetting.getId()));
				dbRequest.setDbTranslates(dbTranslates);
				responseHelper.putData(dbRequest, "Success", "");	
			}
		} catch (Exception e) {
			responseHelper.putData(new ArrayList<Object>(), "Failed", e.getMessage());
			log.debug(e.getMessage());
		}
		
		return responseHelper.getResponseData();
	}
	
	public Map<String, List<?>> editDBSetting(HttpServletRequest request, String json, MultipartFile[] files, HttpSession httpSession){
		try {
			List<DBTranslate> dbTranslates = new ArrayList<DBTranslate>();
			DBRequest dbRequest = new ObjectMapper().readValue(json, DBRequest.class);
			DBSetting dbSetting = dbRequest.getDbSetting();
			Boolean error = false;
			
			DBSetting beforeSetting = dbSettingRepository.findOne(dbSetting.getId());
			if (dbSetting.getId() == null) {
				responseHelper.putData(new ArrayList<Object>(), "Failed", "id for dbSetting must not be null.");
				error = true;
			}
			
			if (beforeSetting == null) {
				responseHelper.putData(new ArrayList<Object>(), "Failed", "Id not found in table dbservice.");
				error = true;
			}
			
			if (!error) {
				String allFileName = "";
				String sql = "update dbsetting set db_host = '"+dbSetting.getDbHost()+"', db_name = '"+dbSetting.getDbName()+"', "
						     +" db_password = '"+dbSetting.getDbPassword()+"', db_table_name = '"+dbSetting.getDbTableName()+"', "
						     +" db_type = '"+dbSetting.getDbType()+"', db_username = '"+dbSetting.getDbUsername()+"' "
						     +" where id = "+dbSetting.getId();
				globalHelper.updateQuery(sql);
				globalHelper.closeConnection();	
				
				sql = "delete from dbtranslate where id_service ="+dbSetting.getId();
				globalHelper.updateQuery(sql);
				globalHelper.closeConnection();
				
				log.debug("Start to save data");
				for (DBTranslate dbTranslate : dbRequest.getDbTranslates()) {
					DBTranslate dbTranslateTmp = null;
					
					dbTranslate.setIdService(dbSetting.getId());
					dbTranslateTmp = dbTranslateRepository.save(dbTranslate);
					dbTranslates.add(dbTranslateTmp);
				}
				log.debug("Done to save data");
				
				if(files != null){
					log.debug("Start to save file");
					Integer i = 1;
					for(MultipartFile file : files){
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");  
						Date date = new Date();
						String fileName = dbSetting.getId()+"_"+dateFormat.format(date)+"_"+i+"."+globalHelper.getFileExtension(file.getOriginalFilename());
						if(globalHelper.getFileExtension(file.getOriginalFilename()).equals("xlsx") || globalHelper.getFileExtension(file.getOriginalFilename()).equals("xls")){
							File fileTransfer = new File(httpSession.getServletContext().getRealPath("/uploadFile/excel/"), fileName);
							file.transferTo(fileTransfer);
						}else if(globalHelper.getFileExtension(file.getOriginalFilename()).equals("txt")){
							File fileTransfer = new File(httpSession.getServletContext().getRealPath("/uploadFile/text/"), fileName);
							file.transferTo(fileTransfer);
						}
						allFileName += fileName+"%";
						i++;
					}
					allFileName += ";";
					allFileName = allFileName.replace("%;", "");
					
					String[] tmpFileName = beforeSetting.getFiles().split("%");
					log.debug(tmpFileName.length);
					
					for (String file : tmpFileName) {
						if(globalHelper.getFileExtension(file).equals("xlsx") || globalHelper.getFileExtension(file).equals("xls")){
							File fileDelete = new File(httpSession.getServletContext().getRealPath("/uploadFile/excel/"), file);
							log.debug("Delete file: "+fileDelete);
							fileDelete.delete();
						}else if(globalHelper.getFileExtension(file).equals("txt")){
							File fileDelete = new File(httpSession.getServletContext().getRealPath("/uploadFile/text/"), file);
							log.debug("Delete file: "+fileDelete);
							fileDelete.delete();
						}
						//File fileDelete = new File(httpSession.getServletContext().getRealPath("/uploadFile/excel/"), file);
						//log.debug("Delete file: "+fileDelete);
						//fileDelete.delete();
					}
					sql = "update dbsetting set files = '"+allFileName+"', status = 'Empty' where id = "+dbSetting.getId();
					globalHelper.updateQuery(sql);
					globalHelper.closeConnection();
				}
				dbRequest.setDbSetting(dbSettingRepository.findOne(dbSetting.getId()));
				dbRequest.setDbTranslates(dbTranslates);
				responseHelper.putData(dbRequest, "Success", "");	
			}
			
		} catch (Exception e) {
			responseHelper.putData(new ArrayList<Object>(), "Failed", e.getMessage());
		}
		return responseHelper.getResponseData();
	}
	
	public Map<String, List<?>> deleteDBSetting(HttpServletRequest request, DBSetting dbSetting){
		try {
			DBSetting beforeDBSetting = new DBSetting();
			beforeDBSetting = dbSettingRepository.findOne(dbSetting.getId());
			
			if(beforeDBSetting == null){
				responseHelper.putData(new ArrayList<Object>(), "Failed", "Id not found in table dbservice.");
			}else{
				dbSettingRepository.delete(dbSetting);
				String sql = "delete from dbtranslate where id_service ="+dbSetting.getId();
				globalHelper.updateQuery(sql);
				globalHelper.closeConnection();
				
				sql = "DELETE FROM dbtranslate WHERE id_service = "+dbSetting.getId();
				globalHelper.updateQuery(sql);
				globalHelper.closeConnection();
				
				responseHelper.putData(new ArrayList<Object>(), "Success", "");
			}
		} catch (Exception e) {
			responseHelper.putData(new ArrayList<Object>(), "Failed", e.getMessage());
			log.debug(e.getMessage());
		}
		
		return responseHelper.getResponseData();
	}
	
	public Map<String, List<?>> dbCopyStart(HttpServletRequest request, DBCopyStartRequest dbCopyStartRequest, HttpSession session, Boolean scheduler){
		try {
			DBSetting dbSetting = dbSettingRepository.findOne(dbCopyStartRequest.getIdService());			
			Boolean error = false;
			Integer countExDatabase = 0;
			
			if(dbSetting == null){
				responseHelper.putData(dbCopyStartRequest, "Failed", "idService is not exist in database.");
				error = true;
			}
			
			InstitusiList institusiList = institusiListRepository.findOne(dbSetting.getIdInstitusi());
			if(institusiList == null){
				responseHelper.putData(dbCopyStartRequest, "Failed", "idInstitusi is not exist in database.");
				error = true;
			}
			
			if(!error){
				if (dbSetting.getDbType().equals("mysql") || dbSetting.getDbType().equals("oracle")) {
					ConnectionHelper connectionHelper = new ConnectionHelper(dbSetting);
					Connection connection = connectionHelper.SetConnnection();
					List<DBTranslate> dbTranslates = new ArrayList<DBTranslate>();
					
					log.debug("DBType "+dbSetting.getDbType());
					
					dbTranslates = dbTranslateRepository.findByIdService(dbSetting.getId());
					String select = "";
					String sql = "SELECT COUNT(*) AS count FROM "+dbSetting.getDbTableName();
					if (scheduler) {
						sql += this.getWhereQuery(dbTranslates);
					}
					select = connectionHelper.setSelectedField(dbTranslates);
					
					ResultSet resultSet = connectionHelper.executeQuery(connection, sql);
					resultSet.next();
					countExDatabase = new Integer(resultSet.getString("count"));
					connection.close();
					
					dbTranslates = dbTranslateRepository.findByIdService(dbSetting.getId());
					
					if(!scheduler){
						sql = "delete from institusi"+institusiList.getInstitusiTabel()+" where id_service = "+dbSetting.getId();
						globalHelper.updateQuery(sql);
						globalHelper.closeConnection();						
					}
						
					log.debug("Start to copy database");
					
					for (int i = 0; i < countExDatabase; i++) {
						connection = connectionHelper.SetConnnection();
						if (dbSetting.getDbType().equals("mysql")) {						
							if (scheduler) {
								sql = "SELECT "+select+" FROM "+dbSetting.getDbTableName()+this.getWhereQuery(dbTranslates)+" LIMIT 1 OFFSET "+i;
							}else{
								sql = "SELECT "+select+" FROM "+dbSetting.getDbTableName()+" LIMIT 1 OFFSET "+i;								
							}
						} if (dbSetting.getDbType().equals("oracle")) {	
							if (scheduler) {
								sql = "SELECT "+select+" FROM (SELECT ROWNUM RNUM, A.* FROM "+dbSetting.getDbTableName()+" A WHERE ROWNUM <= "+(i+1)+") "+this.getWhereQuery(dbTranslates)+" AND RNUM >="+(i+1);
							}else{
								sql = "SELECT "+select+" FROM (SELECT ROWNUM RNUM, A.* FROM "+dbSetting.getDbTableName()+" A WHERE ROWNUM <= "+(i+1)+") WHERE RNUM >="+(i+1);								
							}
						}
						
						log.debug("SQL Select: "+sql);
						resultSet = connectionHelper.executeQuery(connection, sql);
						resultSet.next();
						
						String field = "";
						String value = "";
						
						for (DBTranslate dbTranslate : dbTranslates) {
							field += "`"+dbTranslate.getTargetTableName()+"`,";
							value += "'"+resultSet.getString(dbTranslate.getTargetTableName())+"',";
						}
						field += "`id_service`";
						value += "'"+dbSetting.getId()+"'";
						
						sql = "insert into institusi"+institusiList.getInstitusiTabel() +"("+field+") values("+value+")";
						log.debug("SQL Insert: "+ sql);
						globalHelper.updateQuery(sql);
						globalHelper.closeConnection();
						
						connection.close();
					}
					
					log.debug("Finish to copy database");
					
					log.debug("Update status to ready");
					sql = "update dbsetting set status = 'Ready' where id = "+dbSetting.getId();
					globalHelper.updateQuery(sql);
					globalHelper.closeConnection();
					
				}else if(dbSetting.getDbType().equals("excel")){
					List<DBTranslate> dbTranslates = new ArrayList<DBTranslate>();
					dbTranslates = dbTranslateRepository.findByIdService(dbSetting.getId());
					
					log.debug("DBType "+dbSetting.getDbType());
					
					Integer dataPosition = 1;					
					
					String sql = "delete from institusi"+institusiList.getInstitusiTabel()+" where id_service = "+dbSetting.getId();
					globalHelper.updateQuery(sql);
					globalHelper.closeConnection();
					
					sql = "select * from dbsetting where id="+dbSetting.getId();
					ResultSet result = globalHelper.executeQuery(sql);
					result.next();
					
					log.debug(result.getString("files"));
										
					if(result.getString("files") != null){
						String[] files = result.getString("files").split("%");
						for (String fileString : files) {						
							File fileExcel = new File(servletContext.getRealPath("/uploadFile/excel/"+fileString));
							ReaderHelper readerHelper = new ReaderHelper(fileExcel);
							Map<String, String> indexDataExcel = new HashMap<String, String>();
							
							for (DBTranslate dbTranslate : dbTranslates) {
								if (dbTranslate.getOriginTableName().indexOf("[constan]") != -1) {
									String originTableName = dbTranslate.getOriginTableName().replace("[constan]", "");
									
									indexDataExcel.put(dbTranslate.getTargetTableName(), originTableName);
								}else{
									String originTableName = dbTranslate.getOriginTableName();
									Integer index = readerHelper.getCellPosition(originTableName, dataPosition);
									indexDataExcel.put(dbTranslate.getTargetTableName(), ""+index);
								}
							}
							
							
							log.debug(indexDataExcel);
							log.debug("Count Row "+fileString+" => "+readerHelper.getRowCount());
							countExDatabase += (readerHelper.getRowCount()-4);
							
							log.debug("Start to copy database");
							
							for (int i = (dataPosition+2); i < (readerHelper.getRowCount())-1; i++) {
								String field = "";
								String value = "";
								
								for(Map.Entry<String, String> excelValue : indexDataExcel.entrySet()){
									String data = "";
									if(excelValue.getValue().indexOf(",") != -1){
										String[] indexPosition = excelValue.getValue().split(",");
										Integer indexRow = Integer.parseInt(indexPosition[0]);
										Integer indexColoum = Integer.parseInt(indexPosition[1]);
										
										data = readerHelper.getVelueUsingIndex(indexRow, indexColoum);
										log.debug("Row: "+indexRow+" Coloum: "+indexColoum+ " Value: "+readerHelper.getVelueUsingIndex(indexRow, indexColoum));
									}else{
										data = readerHelper.getVelueUsingIndex(i, Integer.parseInt(excelValue.getValue()));
									}
									
									field += "`"+excelValue.getKey()+"`,";
									value += "'"+data+"',";
								}
								
								field += "`id_service`";
								value += "'"+dbSetting.getId()+"'";
								
								sql = "insert into institusi"+institusiList.getInstitusiTabel() +"("+field+") values("+value+")";
								log.debug("SQL Insert: "+ sql);
								globalHelper.updateQuery(sql);
								globalHelper.closeConnection();								
							}
						}
						
						log.debug("Finish to copy database");
						
						log.debug("Update status to ready");
						sql = "update dbsetting set status = 'Ready' where id = "+dbSetting.getId();
						globalHelper.updateQuery(sql);
						globalHelper.closeConnection();
					}else{
						responseHelper.putData(new ArrayList<Object>(), "Failed", "File "+countExDatabase+" not found.");
					}
					
				}else if(dbSetting.getDbType().equals("excel_scheduller") && scheduler){
					File folder = new File(servletContext.getRealPath(dbSetting.getFilesDirectory()));
					File[] listOfFiles = folder.listFiles();
					
					List<DBTranslate> dbTranslates = new ArrayList<DBTranslate>();
					dbTranslates = dbTranslateRepository.findByIdService(dbSetting.getId());
					
					log.debug("DBType "+dbSetting.getDbType());
					Integer dataPosition = 1; 
					for (int i = 0; i < listOfFiles.length; i++) {
						File fileExcel = new File(servletContext.getRealPath(dbSetting.getFilesDirectory()+"/"+listOfFiles[i]));
						ReaderHelper readerHelper = new ReaderHelper(fileExcel);
						Map<String, String> indexDataExcel = new HashMap<String, String>();
						
						for (DBTranslate dbTranslate : dbTranslates) {
							if (dbTranslate.getOriginTableName().indexOf("[constan]") != -1) {
								String originTableName = dbTranslate.getOriginTableName().replace("[constan]", "");
								
								indexDataExcel.put(dbTranslate.getTargetTableName(), originTableName);
							}else{
								String originTableName = dbTranslate.getOriginTableName();
								Integer index = readerHelper.getCellPosition(originTableName, dataPosition);
								indexDataExcel.put(dbTranslate.getTargetTableName(), ""+index);
							}
						}
										
						log.debug(indexDataExcel);
						log.debug("Count Row "+listOfFiles[i]+" => "+readerHelper.getRowCount());
						countExDatabase += (readerHelper.getRowCount()-4);
						
						log.debug("Start to copy database");
						
						for (int j = (dataPosition+2); j < (readerHelper.getRowCount())-1; j++) {
							String field = "";
							String value = "";
							
							for(Map.Entry<String, String> excelValue : indexDataExcel.entrySet()){
								String data = "";
								if(excelValue.getValue().indexOf(",") != -1){
									String[] indexPosition = excelValue.getValue().split(",");
									Integer indexRow = Integer.parseInt(indexPosition[0]);
									Integer indexColoum = Integer.parseInt(indexPosition[1]);
									
									data = readerHelper.getVelueUsingIndex(indexRow, indexColoum);
									log.debug("Row: "+indexRow+" Coloum: "+indexColoum+ " Value: "+readerHelper.getVelueUsingIndex(indexRow, indexColoum));
								}else{
									data = readerHelper.getVelueUsingIndex(i, Integer.parseInt(excelValue.getValue()));
								}
								
								field += "`"+excelValue.getKey()+"`,";
								value += "'"+data+"',";
							}
							
							field += "`id_service`";
							value += "'"+dbSetting.getId()+"'";
							
							String sql = "insert into institusi"+institusiList.getInstitusiTabel() +"("+field+") values("+value+")";
							log.debug("SQL Insert: "+ sql);
							globalHelper.updateQuery(sql);
							globalHelper.closeConnection();								
						}
					}
					
					log.debug("Finish to copy database");
					
					log.debug("Update status to ready");
					String sql = "update dbsetting set status = 'Ready' where id = "+dbSetting.getId();
					globalHelper.updateQuery(sql);
					globalHelper.closeConnection();
					
				}
				
				responseHelper.putData(new ArrayList<Object>(), "Success", countExDatabase+" data migrated.");
			}
		} catch (Exception e) {
			responseHelper.putData(new ArrayList<Object>(), "Failed", e.getMessage());
			e.printStackTrace();
		}
		
		return responseHelper.getResponseData();
	}
	
	public Map<String, List<?>> clearTmpService(HttpServletRequest request, DBSetting dbSetting){
		try {
			DBSetting beforeDBDbSetting = new DBSetting();
			beforeDBDbSetting = dbSettingRepository.findOne(dbSetting.getId());
			
			log.debug(dbSetting);
			if (beforeDBDbSetting == null) {
				responseHelper.putData(new ArrayList<Object>(), "Failed", "Id not found in table dbsetting.");
			}else{
				for (int i = 1; i <= 6; i++) {
					String sql = "delete from institusi"+i+" where id_service = "+dbSetting.getId();
					globalHelper.updateQuery(sql);
					globalHelper.closeConnection();
				}
				
				String sql = "update dbsetting set status = 'Empty' where id = "+dbSetting.getId();
				globalHelper.updateQuery(sql);
				globalHelper.closeConnection();				
			}
			
			
			responseHelper.putData(new ArrayList<Object>(), "Success", "");
		} catch (Exception e) {
			 responseHelper.putData(new ArrayList<Object>(), "Failed", e.getMessage());
			 log.debug(e);
		}
		return responseHelper.getResponseData();
	}
	
	public String getWhereQuery(List<DBTranslate> dbTranslates) {
		String whereQuery = "";
		
		for (DBTranslate dbTranslate : dbTranslates) {
			if(dbTranslate.getTargetTableName().equals("local_date")){
				whereQuery = " where "+dbTranslate.getOriginTableName()+" like '%"+utilHelper.convertDateToDB()+"%'";
				break;
			}
		}
		
		return whereQuery;
	}
}
