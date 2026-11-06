package com.cloudhis.mapper;

import org.apache.ibatis.annotations.*;
import java.util.*;

@Mapper
public interface AuthMapper {
    String PUBLIC = "u.id,u.username,u.display_name,u.role_code,u.department_id,u.active,d.name AS department_name";
    @Select("SELECT u.*,d.name AS department_name FROM app_user u LEFT JOIN department d ON d.id=u.department_id WHERE u.username=#{username}")
    Map<String,Object> byUsername(String username);
    @Select("SELECT "+PUBLIC+" FROM app_user u LEFT JOIN department d ON d.id=u.department_id WHERE u.id=#{id}")
    Map<String,Object> byId(String id);
    @Select("SELECT password_hash FROM app_user WHERE id=#{id}") String password(String id);
    @Select("SELECT "+PUBLIC+" FROM app_user u LEFT JOIN department d ON d.id=u.department_id ORDER BY u.created_at,u.username")
    List<Map<String,Object>> users();
    @Insert("INSERT INTO app_user(id,username,password_hash,display_name,role_code,department_id,active) VALUES(#{id},#{username},#{password_hash},#{display_name},#{role_code},#{department_id},TRUE)")
    void insert(Map<String,Object> user);
    @Update("UPDATE app_user SET password_hash=#{hash} WHERE id=#{id}") void changePassword(@Param("id") String id,@Param("hash") String hash);
    @Update("UPDATE app_user SET active=#{active} WHERE id=#{id}") void setActive(@Param("id") String id,@Param("active") boolean active);
    @Insert("INSERT INTO audit_log(id,actor_id,action_name,entity_id,detail) VALUES(#{id},#{actor_id},#{action_name},#{entity_id},#{detail})") void audit(Map<String,Object> audit);
    @Select("SELECT a.*,u.display_name AS actor_name FROM audit_log a JOIN app_user u ON u.id=a.actor_id ORDER BY a.created_at DESC,a.id LIMIT #{size} OFFSET #{offset}")
    List<Map<String,Object>> auditLogs(@Param("size") int size,@Param("offset") int offset);
    @Select("SELECT COUNT(*) FROM audit_log") long countLogs();
}
