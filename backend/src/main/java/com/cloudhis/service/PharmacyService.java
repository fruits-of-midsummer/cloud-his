package com.cloudhis.service;

import com.cloudhis.mapper.*;
import com.cloudhis.web.Requests;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.cloudhis.common.Values.*;
import static com.cloudhis.common.BusinessException.check;

@Service
public class PharmacyService {
    private final CatalogMapper catalog;private final OrderMapper orders;private final FinanceMapper finance;private final AuthService auth;
    public PharmacyService(CatalogMapper c,OrderMapper o,FinanceMapper f,AuthService a){catalog=c;orders=o;finance=f;auth=a;}
    private void movement(Map<String,Object> user,String medicine,String order,String kind,int qty,String reason) {
        finance.movement(map("id",id(),"medicine_id",medicine,"order_id",order,"kind",kind,"quantity",qty,"reason",reason,"operator_id",str(user,"id")));
    }
    @Transactional public void dispense(Map<String,Object> user,String id) {
        Map<String,Object> order=must(orders.lock(id),"药品项目不存在");
        check(str(order,"kind").equals("DRUG") && str(order,"billing_status").equals("PAID") && str(order,"execution_status").equals("PENDING"),"仅已缴费且未发放的药品可发药");
        int qty=(int)num(order,"quantity");String medicine=str(order,"medicine_id");
        check(catalog.takeStock(medicine,qty)==1,"药品库存不足或已停用，本次发药未生效");
        movement(user,medicine,id,"DISPENSE",-qty,"处方发药");orders.execution(id,"DISPENSED");auth.audit(user,"DISPENSE",id,"完成药品发放");
    }
    @Transactional public void returnDrug(Map<String,Object> user,String id,String reason) {
        Map<String,Object> order=must(orders.lock(id),"药品项目不存在");
        check(str(order,"kind").equals("DRUG") && str(order,"billing_status").equals("PAID") && str(order,"execution_status").equals("DISPENSED"),"仅已发且尚未退回的药品可以退药");
        int qty=(int)num(order,"quantity");String medicine=str(order,"medicine_id");
        check(catalog.addStock(medicine,qty)==1,"库存数量超出限制");movement(user,medicine,id,"RETURN",qty,reason);orders.execution(id,"RETURNED");auth.audit(user,"DRUG_RETURN",id,reason);
    }
    @Transactional public void stock(Map<String,Object> user,Requests.Stock req) {
        must(catalog.medicine(req.medicineId()),"药品不存在");check(catalog.addStock(req.medicineId(),req.quantity())==1,"库存数量超出限制");
        movement(user,req.medicineId(),null,"STOCK_IN",req.quantity(),req.reason());auth.audit(user,"STOCK_IN",req.medicineId(),req.reason());
    }
}
