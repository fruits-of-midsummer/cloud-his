package com.cloudhis.service;

import com.cloudhis.mapper.*;
import com.cloudhis.web.Requests;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.cloudhis.common.Values.*;
import static com.cloudhis.common.BusinessException.check;

@Service
public class ClinicalService {
    private final EncounterMapper encounters;private final OrderMapper orders;private final CatalogMapper catalog;
    private final AuthService auth;private final AccessService access;
    public ClinicalService(EncounterMapper e,OrderMapper o,CatalogMapper c,AuthService a,AccessService x){encounters=e;orders=o;catalog=c;auth=a;access=x;}
    private void writable(Map<String,Object> user,String id) {
        access.clinical(user,id); Map<String,Object> e=must(encounters.lock(id),"就诊不存在");
        check(!Set.of("CANCELLED","COMPLETED").contains(str(e,"status")),"已退号或已结束就诊不能修改");
    }
    @Transactional public Map<String,Object> record(Map<String,Object> user,String id,Requests.Record req) {
        writable(user,id);
        Map<String,Object> row=map("encounter_id",id,"chief_complaint",req.chiefComplaint(),"present_illness",req.presentIllness(),"treatment_history",req.treatmentHistory(),"past_history",req.pastHistory(),"allergy_history",req.allergyHistory(),"physical_exam",req.physicalExam(),"initial_diagnosis",req.initialDiagnosis(),"suggestions",req.suggestions(),"version",req.version(),"updated_by",str(user,"id"));
        Map<String,Object> existing=encounters.record(id);
        if(existing==null){check(req.version()==0,"病历版本已变化，请刷新");encounters.addRecord(row);}
        else check(encounters.updateRecord(row)==1,"病历已被更新，请刷新后再编辑");
        if(str(encounters.get(id),"status").equals("REGISTERED")) encounters.status(id,"IN_PROGRESS");
        auth.audit(user,"RECORD_SAVE",id,"保存门诊病历");return encounters.record(id);
    }
    @Transactional public Map<String,Object> diagnose(Map<String,Object> user,String id,Requests.Diagnosis req) {
        writable(user,id);must(encounters.record(id),"请先填写病历和初步诊断");
        Map<String,Object> row=map("encounter_id",id,"disease_code",req.diseaseCode(),"disease_name",req.diseaseName(),"opinion",req.opinion(),"doctor_id",str(user,"id"));
        if(encounters.diagnosis(id)==null) encounters.addDiagnosis(row);else encounters.updateDiagnosis(row);
        encounters.status(id,"DIAGNOSED");auth.audit(user,"DIAGNOSIS_SAVE",id,"提交门诊确诊");return encounters.diagnosis(id);
    }
    @Transactional public List<Map<String,Object>> order(Map<String,Object> user,Requests.Order req) {
        writable(user,req.encounterId());must(encounters.record(req.encounterId()),"请先保存病历和初步诊断");
        check(new HashSet<>(req.itemIds()).size()==req.itemIds().size(),"所选项目重复");
        for(String itemId:req.itemIds()) {
            Map<String,Object> item=must(catalog.item(itemId),"项目不存在");check(active(item),"项目已停用");
            if(str(item,"kind").equals("TREAT")) must(encounters.diagnosis(req.encounterId()),"处置申请需要先完成门诊确诊");
            orders.add(map("id",id(),"encounter_id",req.encounterId(),"kind",str(item,"kind"),"service_item_id",itemId,"item_name",str(item,"name"),"unit_price_cents",num(item,"price_cents"),"quantity",1,"amount_cents",num(item,"price_cents"),"billing_status","UNPAID","execution_status","PENDING","department_id",str(item,"department_id"),"doctor_id",str(user,"id"),"purpose",req.purpose(),"body_part",req.bodyPart(),"notes",req.notes()));
        }
        auth.audit(user,"ORDER_CREATE",req.encounterId(),"开立医技项目数量："+req.itemIds().size());return orders.forEncounter(req.encounterId());
    }
    @Transactional public List<Map<String,Object>> prescription(Map<String,Object> user,Requests.Prescription req) {
        writable(user,req.encounterId());must(encounters.diagnosis(req.encounterId()),"开立处方需要先完成门诊确诊");
        String prescriptionId=id();orders.prescription(map("id",prescriptionId,"encounter_id",req.encounterId(),"doctor_id",str(user,"id")));
        Set<String> seen=new HashSet<>();
        for(Requests.Drug drug:req.items()) {
            check(seen.add(drug.medicineId()),"同一处方不可重复添加同一种药品");
            Map<String,Object> medicine=must(catalog.medicine(drug.medicineId()),"药品不存在");check(active(medicine),"药品已停用");
            orders.add(map("id",id(),"encounter_id",req.encounterId(),"kind","DRUG","medicine_id",drug.medicineId(),"prescription_id",prescriptionId,"item_name",str(medicine,"name"),"unit_price_cents",num(medicine,"price_cents"),"quantity",drug.quantity(),"amount_cents",Math.multiplyExact(num(medicine,"price_cents"),drug.quantity()),"billing_status","UNPAID","execution_status","PENDING","doctor_id",str(user,"id"),"drug_usage",drug.drugUsage(),"dosage",drug.dosage(),"frequency",drug.frequency(),"days",drug.days(),"notes",drug.notes()));
        }
        auth.audit(user,"PRESCRIPTION_CREATE",prescriptionId,"开立处方");return orders.forEncounter(req.encounterId());
    }
    @Transactional public void cancelOrder(Map<String,Object> user,String id,String reason) {
        Map<String,Object> original=must(orders.get(id),"申请不存在");writable(user,str(original,"encounter_id"));
        Map<String,Object> row=orders.lock(id);check(!str(row,"kind").equals("REG"),"挂号项目不能在此撤销");
        check(str(row,"billing_status").equals("UNPAID") && str(row,"execution_status").equals("PENDING"),"仅未收费未执行项目可以直接撤销；已收费项目请先办理退费");
        orders.execution(id,"CANCELLED");auth.audit(user,"ORDER_CANCEL",id,reason);
    }
    @Transactional public void finish(Map<String,Object> user,String id) {
        writable(user,id);must(encounters.diagnosis(id),"请先完成门诊确诊");
        check(orders.forEncounter(id).stream().filter(o->!str(o,"kind").equals("REG")).allMatch(o->Set.of("COMPLETED","DISPENSED","CANCELLED").contains(str(o,"execution_status"))),"仍有未完成的收费、医技或药房项目");
        encounters.status(id,"COMPLETED");auth.audit(user,"ENCOUNTER_FINISH",id,"结束本次就诊");
    }
}
