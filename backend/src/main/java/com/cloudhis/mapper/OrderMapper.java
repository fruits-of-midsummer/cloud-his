package com.cloudhis.mapper;

import org.apache.ibatis.annotations.*;
import java.util.*;

@Mapper
public interface OrderMapper {
    String SELECT="SELECT o.*,p.case_no,p.name AS patient_name,e.visit_no,u.display_name AS executor_name,r.content AS result_content,r.created_at AS result_at FROM order_item o JOIN encounter e ON e.id=o.encounter_id JOIN patient p ON p.id=e.patient_id LEFT JOIN app_user u ON u.id=o.executor_id LEFT JOIN execution_result r ON r.order_id=o.id ";
    @Insert("INSERT INTO order_item(id,encounter_id,kind,service_item_id,medicine_id,prescription_id,item_name,unit_price_cents,quantity,amount_cents,billing_status,execution_status,department_id,doctor_id,purpose,body_part,notes,drug_usage,dosage,frequency,days) VALUES(#{id},#{encounter_id},#{kind},#{service_item_id},#{medicine_id},#{prescription_id},#{item_name},#{unit_price_cents},#{quantity},#{amount_cents},#{billing_status},#{execution_status},#{department_id},#{doctor_id},#{purpose},#{body_part},#{notes},#{drug_usage},#{dosage},#{frequency},#{days})") void add(Map<String,Object> row);
    @Select("SELECT * FROM order_item WHERE id=#{id} FOR UPDATE") Map<String,Object> lock(String id);
    @Select(SELECT+" WHERE o.id=#{id}") Map<String,Object> get(String id);
    @Select(SELECT+" WHERE o.encounter_id=#{id} ORDER BY o.created_at,o.id") List<Map<String,Object>> forEncounter(String id);
    @Select(SELECT+" WHERE (#{kind}='' OR o.kind=#{kind}) AND (#{dept}='' OR o.department_id=#{dept}) AND (p.name LIKE #{search} OR p.case_no LIKE #{search}) ORDER BY o.created_at,o.id LIMIT #{size} OFFSET #{offset}")
    List<Map<String,Object>> list(@Param("kind") String kind,@Param("dept") String dept,@Param("search") String search,@Param("size") int size,@Param("offset") int offset);
    @Select("SELECT COUNT(*) FROM order_item o JOIN encounter e ON e.id=o.encounter_id JOIN patient p ON p.id=e.patient_id WHERE (#{kind}='' OR o.kind=#{kind}) AND (#{dept}='' OR o.department_id=#{dept}) AND (p.name LIKE #{search} OR p.case_no LIKE #{search})") long count(@Param("kind") String kind,@Param("dept") String dept,@Param("search") String search);
    @Update("UPDATE order_item SET billing_status=#{status} WHERE id=#{id}") void billing(@Param("id") String id,@Param("status") String status);
    @Update("UPDATE order_item SET execution_status=#{status} WHERE id=#{id}") void execution(@Param("id") String id,@Param("status") String status);
    @Update("UPDATE order_item SET execution_status='REGISTERED',executor_id=#{executor},registered_at=CURRENT_TIMESTAMP WHERE id=#{id} AND execution_status='PENDING' AND billing_status='PAID'") int register(@Param("id") String id,@Param("executor") String executor);
    @Update("UPDATE order_item SET execution_status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE id=#{id}") void complete(String id);
    @Insert("INSERT INTO execution_result(id,order_id,content,author_id) VALUES(#{id},#{order_id},#{content},#{author_id})") void result(Map<String,Object> row);
    @Insert("INSERT INTO prescription(id,encounter_id,doctor_id) VALUES(#{id},#{encounter_id},#{doctor_id})") void prescription(Map<String,Object> row);
    @Select("SELECT COUNT(*) FROM order_item WHERE encounter_id=#{id} AND kind<>'REG' AND execution_status<>'CANCELLED'") int activeCount(String id);
}
