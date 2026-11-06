package com.cloudhis.mapper;

import org.apache.ibatis.annotations.*;
import java.util.*;

@Mapper
public interface EncounterMapper {
    String SELECT="SELECT e.*,p.case_no,p.name AS patient_name,p.gender,p.birth_date,p.age,p.settle_type,p.phone,p.address,u.display_name AS doctor_name,d.name AS department_name,s.visit_date,s.period,s.level_name FROM encounter e JOIN patient p ON p.id=e.patient_id JOIN app_user u ON u.id=e.doctor_id JOIN department d ON d.id=e.department_id JOIN schedule s ON s.id=e.schedule_id ";
    @Select("SELECT * FROM patient WHERE id=#{id}") Map<String,Object> patient(String id);
    @Select("SELECT * FROM patient WHERE name LIKE #{search} OR case_no LIKE #{search} ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}") List<Map<String,Object>> patients(@Param("search") String search,@Param("size") int size,@Param("offset") int offset);
    @Select("SELECT COUNT(*) FROM patient WHERE name LIKE #{search} OR case_no LIKE #{search}") long countPatients(String search);
    @Insert("INSERT INTO patient(id,case_no,name,gender,birth_date,age,settle_type,id_card,address,phone) VALUES(#{id},#{case_no},#{name},#{gender},#{birth_date},#{age},#{settle_type},#{id_card},#{address},#{phone})") void addPatient(Map<String,Object> row);
    @Insert("INSERT INTO encounter(id,visit_no,patient_id,schedule_id,doctor_id,department_id,status,book_requested) VALUES(#{id},#{visit_no},#{patient_id},#{schedule_id},#{doctor_id},#{department_id},'REGISTERED',#{book_requested})") void add(Map<String,Object> row);
    @Select(SELECT+" WHERE e.id=#{id}") Map<String,Object> get(String id);
    @Select("SELECT * FROM encounter WHERE id=#{id} FOR UPDATE") Map<String,Object> lock(String id);
    @Select(SELECT+" WHERE (#{doctor}='' OR e.doctor_id=#{doctor}) AND (p.name LIKE #{search} OR p.case_no LIKE #{search} OR e.visit_no LIKE #{search}) ORDER BY e.created_at DESC,e.id LIMIT #{size} OFFSET #{offset}")
    List<Map<String,Object>> list(@Param("doctor") String doctor,@Param("search") String search,@Param("size") int size,@Param("offset") int offset);
    @Select("SELECT COUNT(*) FROM encounter e JOIN patient p ON p.id=e.patient_id WHERE (#{doctor}='' OR e.doctor_id=#{doctor}) AND (p.name LIKE #{search} OR p.case_no LIKE #{search} OR e.visit_no LIKE #{search})") long count(@Param("doctor") String doctor,@Param("search") String search);
    @Update("UPDATE encounter SET status=#{status} WHERE id=#{id}") void status(@Param("id") String id,@Param("status") String status);
    @Select("SELECT * FROM medical_record WHERE encounter_id=#{id}") Map<String,Object> record(String id);
    @Insert("INSERT INTO medical_record(encounter_id,chief_complaint,present_illness,treatment_history,past_history,allergy_history,physical_exam,initial_diagnosis,suggestions,updated_by) VALUES(#{encounter_id},#{chief_complaint},#{present_illness},#{treatment_history},#{past_history},#{allergy_history},#{physical_exam},#{initial_diagnosis},#{suggestions},#{updated_by})") void addRecord(Map<String,Object> row);
    @Update("UPDATE medical_record SET chief_complaint=#{chief_complaint},present_illness=#{present_illness},treatment_history=#{treatment_history},past_history=#{past_history},allergy_history=#{allergy_history},physical_exam=#{physical_exam},initial_diagnosis=#{initial_diagnosis},suggestions=#{suggestions},updated_by=#{updated_by},version=version+1,updated_at=CURRENT_TIMESTAMP WHERE encounter_id=#{encounter_id} AND version=#{version}") int updateRecord(Map<String,Object> row);
    @Select("SELECT * FROM diagnosis WHERE encounter_id=#{id}") Map<String,Object> diagnosis(String id);
    @Insert("INSERT INTO diagnosis(encounter_id,disease_code,disease_name,opinion,doctor_id) VALUES(#{encounter_id},#{disease_code},#{disease_name},#{opinion},#{doctor_id})") void addDiagnosis(Map<String,Object> row);
    @Update("UPDATE diagnosis SET disease_code=#{disease_code},disease_name=#{disease_name},opinion=#{opinion},doctor_id=#{doctor_id},updated_at=CURRENT_TIMESTAMP WHERE encounter_id=#{encounter_id}") void updateDiagnosis(Map<String,Object> row);
}
