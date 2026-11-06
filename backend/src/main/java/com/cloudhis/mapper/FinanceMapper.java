package com.cloudhis.mapper;

import org.apache.ibatis.annotations.*;
import java.util.*;

@Mapper
public interface FinanceMapper {
    @Select("SELECT * FROM invoice WHERE request_key=#{key}") Map<String,Object> byRequest(String key);
    @Insert("INSERT INTO invoice(id,receipt_no,encounter_id,request_key,fingerprint,total_cents,received_cents,change_cents,pay_method,cashier_id) VALUES(#{id},#{receipt_no},#{encounter_id},#{request_key},#{fingerprint},#{total_cents},#{received_cents},#{change_cents},#{pay_method},#{cashier_id})") void addInvoice(Map<String,Object> row);
    @Insert("INSERT INTO invoice_line(id,invoice_id,order_id,amount_cents) VALUES(#{id},#{invoice_id},#{order_id},#{amount_cents})") void addLine(Map<String,Object> row);
    @Select("SELECT l.*,o.item_name,o.quantity,o.unit_price_cents,o.billing_status,o.execution_status FROM invoice_line l JOIN order_item o ON o.id=l.order_id WHERE l.invoice_id=#{id}") List<Map<String,Object>> lines(String id);
    @Select("SELECT i.*,p.name AS patient_name,p.case_no,u.display_name AS cashier_name FROM invoice i JOIN encounter e ON e.id=i.encounter_id JOIN patient p ON p.id=e.patient_id JOIN app_user u ON u.id=i.cashier_id WHERE i.id=#{id}") Map<String,Object> invoice(String id);
    @Select("SELECT * FROM invoice WHERE encounter_id=#{id} ORDER BY created_at DESC") List<Map<String,Object>> invoices(String id);
    @Select("SELECT * FROM invoice_line WHERE order_id=#{id}") Map<String,Object> lineForOrder(String id);
    @Insert("INSERT INTO refund(id,invoice_line_id,amount_cents,reason,operator_id) VALUES(#{id},#{invoice_line_id},#{amount_cents},#{reason},#{operator_id})") void refund(Map<String,Object> row);
    @Select("SELECT r.*,o.item_name,o.encounter_id FROM refund r JOIN invoice_line l ON l.id=r.invoice_line_id JOIN order_item o ON o.id=l.order_id WHERE o.encounter_id=#{id} ORDER BY r.created_at DESC") List<Map<String,Object>> refunds(String id);
    @Insert("INSERT INTO drug_movement(id,medicine_id,order_id,kind,quantity,reason,operator_id) VALUES(#{id},#{medicine_id},#{order_id},#{kind},#{quantity},#{reason},#{operator_id})") void movement(Map<String,Object> row);
    @Select("SELECT m.*,d.name AS medicine_name,d.code,p.name AS patient_name,p.case_no,u.display_name AS operator_name FROM drug_movement m JOIN medicine d ON d.id=m.medicine_id LEFT JOIN order_item o ON o.id=m.order_id LEFT JOIN encounter e ON e.id=o.encounter_id LEFT JOIN patient p ON p.id=e.patient_id JOIN app_user u ON u.id=m.operator_id WHERE (d.name LIKE #{search} OR d.code LIKE #{search} OR p.name LIKE #{search} OR p.case_no LIKE #{search}) ORDER BY m.created_at DESC,m.id LIMIT #{size} OFFSET #{offset}")
    List<Map<String,Object>> movements(@Param("search") String search,@Param("size") int size,@Param("offset") int offset);
    @Select("SELECT COUNT(*) FROM drug_movement m JOIN medicine d ON d.id=m.medicine_id LEFT JOIN order_item o ON o.id=m.order_id LEFT JOIN encounter e ON e.id=o.encounter_id LEFT JOIN patient p ON p.id=e.patient_id WHERE d.name LIKE #{search} OR d.code LIKE #{search} OR p.name LIKE #{search} OR p.case_no LIKE #{search}") long movementCount(String search);
}
