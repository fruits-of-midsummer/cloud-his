package com.cloudhis.service;

import com.cloudhis.mapper.*;
import com.cloudhis.web.Requests;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.cloudhis.common.Values.*;
import static com.cloudhis.common.BusinessException.check;

@Service
public class AdminService {
    private final AuthMapper users;private final CatalogMapper catalog;private final AuthService auth;
    public AdminService(AuthMapper u,CatalogMapper c,AuthService a){users=u;catalog=c;auth=a;}
    @Transactional public Map<String,Object> user(Map<String,Object> actor,Requests.User req) {
        check(AuthService.ROLES.contains(req.roleCode()),"角色不存在");
        String dept=req.departmentId();
        if(Set.of("DOCTOR","CHECK","LAB","TREAT").contains(req.roleCode())) {
            Map<String,Object> department=must(catalog.department(dept),"请选择所属科室");
            check(str(department,"kind").equals(req.roleCode().equals("DOCTOR")?"OUTPATIENT":req.roleCode()),"岗位与科室类型不匹配");
        } else if(dept!=null && !dept.isBlank()) must(catalog.department(dept),"科室不存在");
        String id=id();users.insert(map("id",id,"username",req.username(),"display_name",req.displayName(),"role_code",req.roleCode(),"department_id",dept==null||dept.isBlank()?null:dept,"password_hash",auth.encode(req.password())));
        auth.audit(actor,"USER_CREATE",id,"创建用户及岗位："+req.roleCode());return users.byId(id);
    }
    @Transactional public void setUserActive(Map<String,Object> actor,String id,boolean active) {
        must(users.byId(id),"用户不存在");check(!id.equals(str(actor,"id")),"不能停用当前登录账号");users.setActive(id,active);auth.audit(actor,"USER_STATUS",id,active?"启用账号":"停用账号");
    }
    @Transactional public void schedule(Map<String,Object> actor,Requests.Schedule req) {
        Map<String,Object> doctor=must(users.byId(req.doctorId()),"医生不存在");check(active(doctor)&&str(doctor,"role_code").equals("DOCTOR"),"请选择有效门诊医生");
        String id=id();catalog.addSchedule(map("id",id,"doctor_id",req.doctorId(),"visit_date",req.visitDate(),"period",req.period(),"level_name",req.levelName(),"fee_cents",req.feeCents(),"quota",req.quota()));auth.audit(actor,"SCHEDULE_CREATE",id,"新增医生排班");
    }
    @Transactional public void item(Map<String,Object> actor,Requests.ServiceItem req) {
        Map<String,Object> dept=must(catalog.department(req.departmentId()),"科室不存在");check(str(dept,"kind").equals(req.kind()),"项目类型与执行科室不匹配");
        String id=id();catalog.addItem(map("id",id,"code",req.code(),"name",req.name(),"kind",req.kind(),"specification",req.specification(),"price_cents",req.priceCents(),"department_id",req.departmentId()));auth.audit(actor,"ITEM_CREATE",id,"新增医技项目");
    }
    @Transactional public void medicine(Map<String,Object> actor,Requests.Medicine req) {
        String id=id();catalog.addMedicine(map("id",id,"code",req.code(),"name",req.name(),"pinyin",req.pinyin(),"unit",req.unit(),"manufacturer",req.manufacturer(),"formulation",req.formulation(),"price_cents",req.priceCents()));auth.audit(actor,"MEDICINE_CREATE",id,"新增药品目录");
    }
    @Transactional public void updateItem(Map<String,Object> actor,String id,Requests.ServiceItem req) {
        must(catalog.item(id),"项目不存在");Map<String,Object> dept=must(catalog.department(req.departmentId()),"科室不存在");check(str(dept,"kind").equals(req.kind()),"项目类型与执行科室不匹配");
        catalog.updateItem(map("id",id,"code",req.code(),"name",req.name(),"kind",req.kind(),"specification",req.specification(),"price_cents",req.priceCents(),"department_id",req.departmentId()));auth.audit(actor,"ITEM_UPDATE",id,"修改目录，历史医嘱价格不变");
    }
    @Transactional public void updateMedicine(Map<String,Object> actor,String id,Requests.Medicine req) {
        must(catalog.medicine(id),"药品不存在");catalog.updateMedicine(map("id",id,"code",req.code(),"name",req.name(),"pinyin",req.pinyin(),"unit",req.unit(),"manufacturer",req.manufacturer(),"formulation",req.formulation(),"price_cents",req.priceCents()));auth.audit(actor,"MEDICINE_UPDATE",id,"修改药品资料，历史处方价格不变");
    }
    @Transactional public void department(Map<String,Object> actor,Requests.Department req) {String id=id();catalog.addDepartment(map("id",id,"code",req.code(),"name",req.name(),"kind",req.kind()));auth.audit(actor,"DEPARTMENT_CREATE",id,"创建科室");}
}
