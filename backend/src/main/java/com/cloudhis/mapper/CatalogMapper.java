package com.cloudhis.mapper;

import org.apache.ibatis.annotations.*;
import java.util.*;

@Mapper
public interface CatalogMapper {
    @Select("SELECT * FROM department ORDER BY code") List<Map<String,Object>> departments();
    @Select("SELECT * FROM department WHERE id=#{id}") Map<String,Object> department(String id);
    @Insert("INSERT INTO department(id,code,name,kind) VALUES(#{id},#{code},#{name},#{kind})") void addDepartment(Map<String,Object> row);
    @Select("SELECT s.*,u.display_name AS doctor_name,u.department_id,d.name AS department_name FROM schedule s JOIN app_user u ON u.id=s.doctor_id JOIN department d ON d.id=u.department_id WHERE s.visit_date BETWEEN #{start} AND #{end} AND u.active=TRUE ORDER BY s.visit_date,s.period,u.display_name")
    List<Map<String,Object>> schedules(@Param("start") String start,@Param("end") String end);
    @Select("SELECT s.*,u.display_name AS doctor_name,u.department_id,u.active AS doctor_active FROM schedule s JOIN app_user u ON u.id=s.doctor_id WHERE s.id=#{id}") Map<String,Object> schedule(String id);
    @Insert("INSERT INTO schedule(id,doctor_id,visit_date,period,level_name,fee_cents,quota) VALUES(#{id},#{doctor_id},#{visit_date},#{period},#{level_name},#{fee_cents},#{quota})") void addSchedule(Map<String,Object> row);
    @Update("UPDATE schedule SET booked=booked+1 WHERE id=#{id} AND booked<quota") int reserve(String id);
    @Update("UPDATE schedule SET booked=booked-1 WHERE id=#{id} AND booked>0") int release(String id);
    @Select("SELECT * FROM service_item ORDER BY kind,code") List<Map<String,Object>> items();
    @Select("SELECT * FROM service_item WHERE id=#{id}") Map<String,Object> item(String id);
    @Insert("INSERT INTO service_item(id,code,name,kind,specification,price_cents,department_id) VALUES(#{id},#{code},#{name},#{kind},#{specification},#{price_cents},#{department_id})") void addItem(Map<String,Object> row);
    @Select("SELECT * FROM medicine ORDER BY code") List<Map<String,Object>> medicines();
    @Select("SELECT * FROM medicine WHERE id=#{id}") Map<String,Object> medicine(String id);
    @Insert("INSERT INTO medicine(id,code,name,pinyin,unit,manufacturer,formulation,price_cents,stock) VALUES(#{id},#{code},#{name},#{pinyin},#{unit},#{manufacturer},#{formulation},#{price_cents},0)") void addMedicine(Map<String,Object> row);
    @Update("UPDATE medicine SET stock=stock-#{quantity} WHERE id=#{id} AND stock>=#{quantity} AND active=TRUE") int takeStock(@Param("id") String id,@Param("quantity") int quantity);
    @Update("UPDATE medicine SET stock=stock+#{quantity} WHERE id=#{id} AND stock+#{quantity}<=2000000000") int addStock(@Param("id") String id,@Param("quantity") int quantity);
    @Select("SELECT * FROM disease WHERE code LIKE #{search} OR name LIKE #{search} ORDER BY code LIMIT 100") List<Map<String,Object>> diseases(String search);
    @Insert("INSERT INTO disease(code,name) VALUES(#{code},#{name})") void addDisease(@Param("code") String code,@Param("name") String name);
    @Select("SELECT COUNT(*) FROM department") int departmentCount();
    @Update("UPDATE service_item SET active=#{active} WHERE id=#{id}") int setItemActive(@Param("id") String id,@Param("active") boolean active);
    @Update("UPDATE medicine SET active=#{active} WHERE id=#{id}") int setMedicineActive(@Param("id") String id,@Param("active") boolean active);
    @Update("UPDATE service_item SET code=#{code},name=#{name},kind=#{kind},specification=#{specification},price_cents=#{price_cents},department_id=#{department_id} WHERE id=#{id}") int updateItem(Map<String,Object> row);
    @Update("UPDATE medicine SET code=#{code},name=#{name},pinyin=#{pinyin},unit=#{unit},manufacturer=#{manufacturer},formulation=#{formulation},price_cents=#{price_cents} WHERE id=#{id}") int updateMedicine(Map<String,Object> row);
}
