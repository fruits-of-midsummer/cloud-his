package com.cloudhis.web;

import com.cloudhis.common.*;
import com.cloudhis.mapper.*;
import com.cloudhis.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.ResponseEntity;
import java.time.*;
import java.util.*;
import static com.cloudhis.common.Values.*;

@RestController
@RequestMapping("/api")
public class HisController {
    @Autowired private ApplicationAvailability availability;
    private final AuthService auth;private final AccessService access;private final RegistrationService registration;private final ClinicalService clinical;
    private final BillingService billing;private final ExecutionService execution;private final PharmacyService pharmacy;private final AdminService admin;
    private final AuthMapper users;private final CatalogMapper catalog;private final EncounterMapper encounters;private final OrderMapper orders;private final FinanceMapper finance;private final JdbcTemplate jdbc;
    public HisController(AuthService a,AccessService x,RegistrationService r,ClinicalService c,BillingService b,ExecutionService e,PharmacyService p,AdminService m,AuthMapper u,CatalogMapper l,EncounterMapper n,OrderMapper o,FinanceMapper f,JdbcTemplate j) {auth=a;access=x;registration=r;clinical=c;billing=b;execution=e;pharmacy=p;admin=m;users=u;catalog=l;encounters=n;orders=o;finance=f;jdbc=j;}
    private int size(int size) { return Math.max(1,Math.min(100,size)); }
    private int offset(int page,int size) { if(page<1 || page>1000000) throw new BusinessException(400,"页码无效");return (page-1)*size; }
    private String search(String q) { if(q.length()>100) throw new BusinessException(400,"搜索内容过长");return "%"+q.trim()+"%"; }
    private Map<String,Object> page(Object list,long total,int page,int size) { return map("items",list,"total",total,"page",page,"size",size); }
    @GetMapping("/health") Object health() {
        jdbc.queryForObject("SELECT 1",Integer.class);
        if(availability.getReadinessState()!=ReadinessState.ACCEPTING_TRAFFIC) return ResponseEntity.status(503).body(Map.of("status","STARTING"));
        return Map.of("status","UP","version","1.0.0");
    }
    @GetMapping("/dashboard") Object dashboard(HttpSession s) {
        Map<String,Object> user=auth.current(s);String role=str(user,"role_code");String uid=str(user,"id");
        if(Set.of("ADMIN","DOCTOR","CASHIER").contains(role)) {
            String scope=role.equals("DOCTOR")?" WHERE doctor_id=?":"";Object[] params=role.equals("DOCTOR")?new Object[]{uid}:new Object[]{};
            List<Map<String,Object>> stats=jdbc.queryForList("SELECT status,COUNT(*) AS total FROM encounter"+scope+" GROUP BY status",params);
            return map("stats",stats,"recent",encounters.list(role.equals("DOCTOR")?uid:"","%",8,0));
        }
        String scope=role.equals("PHARMACY")?"kind='DRUG'":"kind=? AND department_id=?";Object[] params=role.equals("PHARMACY")?new Object[]{}:new Object[]{role,str(user,"department_id")};
        return map("stats",jdbc.queryForList("SELECT execution_status AS status,COUNT(*) AS total FROM order_item WHERE "+scope+" GROUP BY execution_status",params),"recent",orders.list(role.equals("PHARMACY")?"DRUG":role,role.equals("PHARMACY")?"":str(user,"department_id"),"%",8,0));
    }
    @GetMapping("/catalog") Object catalog(HttpSession s) {
        auth.current(s);List<Map<String,Object>> staff=users.users().stream().filter(Values::active).toList();
        return map("departments",catalog.departments(),"staff",staff,"items",catalog.items(),"medicines",catalog.medicines());
    }
    @GetMapping("/schedules") Object schedules(@RequestParam(defaultValue="") String start,@RequestParam(defaultValue="") String end,HttpSession s) {
        auth.current(s);LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));LocalDate from=start.isBlank()?today:LocalDate.parse(start);LocalDate to=end.isBlank()?from:LocalDate.parse(end);
        if(to.isBefore(from)||to.isAfter(from.plusDays(62))) throw new BusinessException(400,"排班查询范围应在63天以内");return catalog.schedules(from.toString(),to.toString());
    }
    @GetMapping("/diseases") Object diseases(@RequestParam(defaultValue="") String q,HttpSession s) {auth.require(s,"DOCTOR");return catalog.diseases(search(q));}
    @GetMapping("/patients") Object patients(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpSession s) {
        auth.require(s,"CASHIER");size=size(size);return page(encounters.patients(search(q),size,offset(page,size)),encounters.countPatients(search(q)),page,size);
    }
    @PostMapping("/patients") Object patient(@Valid @RequestBody Requests.Patient req,HttpSession s) {return registration.patient(auth.require(s,"CASHIER"),req);}
    @PostMapping("/registrations") Object register(@Valid @RequestBody Requests.Registration req,HttpSession s) {return registration.register(auth.require(s,"CASHIER"),req);}
    @PostMapping("/encounters/{id}/cancel") Object cancel(@PathVariable String id,@Valid @RequestBody Requests.Reason req,HttpSession s) {return registration.cancel(auth.require(s,"CASHIER"),id,req.reason());}
    @GetMapping("/encounters") Object encounters(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpSession s) {
        Map<String,Object> user=auth.require(s,"CASHIER","DOCTOR");String doctor=str(user,"role_code").equals("DOCTOR")?str(user,"id"):"";size=size(size);
        return page(encounters.list(doctor,search(q),size,offset(page,size)),encounters.count(doctor,search(q)),page,size);
    }
    @GetMapping("/encounters/{id}") Object encounter(@PathVariable String id,HttpSession s) {
        Map<String,Object> user=auth.require(s,"CASHIER","DOCTOR");Map<String,Object> row=access.encounter(user,id);
        auth.audit(user,"ENCOUNTER_READ",id,"查询就诊资料");
        // 收费员仅返回费用资料；病历正文只对接诊医生和管理员开放。
        boolean clinical=Set.of("ADMIN","DOCTOR").contains(str(user,"role_code"));
        List<Map<String,Object>> items=orders.forEncounter(id);
        if(!clinical) for(Map<String,Object> item:items) for(String key:List.of("result_content","purpose","body_part","notes","drug_usage","dosage","frequency")) item.remove(key);
        return map("encounter",row,"record",clinical?encounters.record(id):null,"diagnosis",clinical?encounters.diagnosis(id):null,"orders",items,"invoices",finance.invoices(id),"refunds",finance.refunds(id));
    }
    @PutMapping("/encounters/{id}/record") Object record(@PathVariable String id,@Valid @RequestBody Requests.Record req,HttpSession s) {return clinical.record(auth.require(s,"DOCTOR"),id,req);}
    @PostMapping("/encounters/{id}/diagnosis") Object diagnose(@PathVariable String id,@Valid @RequestBody Requests.Diagnosis req,HttpSession s) {return clinical.diagnose(auth.require(s,"DOCTOR"),id,req);}
    @PostMapping("/encounters/{id}/finish") Object finish(@PathVariable String id,HttpSession s) {clinical.finish(auth.require(s,"DOCTOR"),id);return Map.of("ok",true);}
    @PostMapping("/orders") Object order(@Valid @RequestBody Requests.Order req,HttpSession s) {return clinical.order(auth.require(s,"DOCTOR"),req);}
    @PostMapping("/prescriptions") Object prescription(@Valid @RequestBody Requests.Prescription req,HttpSession s) {return clinical.prescription(auth.require(s,"DOCTOR"),req);}
    @PostMapping("/orders/{id}/cancel") Object cancelOrder(@PathVariable String id,@Valid @RequestBody Requests.Reason req,HttpSession s) {clinical.cancelOrder(auth.require(s,"DOCTOR"),id,req.reason());return Map.of("ok",true);}
    @PostMapping("/charges") Object charge(@Valid @RequestBody Requests.Charge req,HttpSession s) {return billing.charge(auth.require(s,"CASHIER"),req);}
    @PostMapping("/orders/{id}/refund") Object refund(@PathVariable String id,@Valid @RequestBody Requests.Reason req,HttpSession s) {return billing.refund(auth.require(s,"CASHIER"),id,req.reason(),false);}
    @GetMapping("/invoices/{id}") Object invoice(@PathVariable String id,HttpSession s) {
        Map<String,Object> user=auth.require(s,"CASHIER","DOCTOR");Map<String,Object> receipt=billing.receipt(id);access.encounter(user,str(receipt,"encounter_id"));return receipt;
    }
    @GetMapping("/orders") Object orders(@RequestParam String kind,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpSession s) {
        if(!Set.of("CHECK","LAB","TREAT","DRUG").contains(kind)) throw new BusinessException(400,"项目类型无效");
        Map<String,Object> user=auth.require(s,kind.equals("DRUG")?"PHARMACY":kind);String dept=str(user,"role_code").equals("ADMIN")||kind.equals("DRUG")?"":str(user,"department_id");size=size(size);
        return page(orders.list(kind,dept,search(q),size,offset(page,size)),orders.count(kind,dept,search(q)),page,size);
    }
    @PostMapping("/orders/{id}/register") Object execute(@PathVariable String id,@Valid @RequestBody Requests.Execute req,HttpSession s) {execution.register(auth.require(s,"CHECK","LAB","TREAT"),id,req);return Map.of("ok",true);}
    @PostMapping("/orders/{id}/result") Object result(@PathVariable String id,@Valid @RequestBody Requests.Result req,HttpSession s) {execution.result(auth.require(s,"CHECK","LAB","TREAT"),id,req);return Map.of("ok",true);}
    @PostMapping("/orders/{id}/dispense") Object dispense(@PathVariable String id,HttpSession s) {pharmacy.dispense(auth.require(s,"PHARMACY"),id);return Map.of("ok",true);}
    @PostMapping("/orders/{id}/return") Object returnDrug(@PathVariable String id,@Valid @RequestBody Requests.Reason req,HttpSession s) {pharmacy.returnDrug(auth.require(s,"PHARMACY"),id,req.reason());return Map.of("ok",true);}
    @PostMapping("/stock/in") Object stock(@Valid @RequestBody Requests.Stock req,HttpSession s) {pharmacy.stock(auth.require(s,"PHARMACY"),req);return Map.of("ok",true);}
    @GetMapping("/stock/movements") Object movements(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpSession s) {
        auth.require(s,"PHARMACY");size=size(size);return page(finance.movements(search(q),size,offset(page,size)),finance.movementCount(search(q)),page,size);
    }
    @GetMapping("/admin/users") Object users(HttpSession s) {auth.require(s);return users.users();}
    @PostMapping("/admin/users") Object user(@Valid @RequestBody Requests.User req,HttpSession s) {return admin.user(auth.require(s),req);}
    @PutMapping("/admin/users/{id}/active") Object active(@PathVariable String id,@RequestBody Map<String,Boolean> req,HttpSession s) {
        if(!req.containsKey("active") || req.get("active")==null) throw new BusinessException(400,"请指定账号状态");admin.setUserActive(auth.require(s),id,req.get("active"));return Map.of("ok",true);
    }
    @PostMapping("/admin/schedules") Object schedule(@Valid @RequestBody Requests.Schedule req,HttpSession s) {admin.schedule(auth.require(s),req);return Map.of("ok",true);}
    @PostMapping("/admin/items") Object item(@Valid @RequestBody Requests.ServiceItem req,HttpSession s) {admin.item(auth.require(s),req);return Map.of("ok",true);}
    @PostMapping("/admin/medicines") Object medicine(@Valid @RequestBody Requests.Medicine req,HttpSession s) {admin.medicine(auth.require(s),req);return Map.of("ok",true);}
    @PutMapping("/admin/items/{id}") Object updateItem(@PathVariable String id,@Valid @RequestBody Requests.ServiceItem req,HttpSession s) {admin.updateItem(auth.require(s),id,req);return Map.of("ok",true);}
    @PutMapping("/admin/medicines/{id}") Object updateMedicine(@PathVariable String id,@Valid @RequestBody Requests.Medicine req,HttpSession s) {admin.updateMedicine(auth.require(s),id,req);return Map.of("ok",true);}
    @PostMapping("/admin/departments") Object department(@Valid @RequestBody Requests.Department req,HttpSession s) {admin.department(auth.require(s),req);return Map.of("ok",true);}
    @PutMapping("/admin/catalog/{kind}/{id}/active") Object catalogActive(@PathVariable String kind,@PathVariable String id,@RequestBody Map<String,Boolean> req,HttpSession s) {
        Map<String,Object> actor=auth.require(s);if(!req.containsKey("active")||req.get("active")==null) throw new BusinessException(400,"请指定启用状态");
        if(kind.equals("medicine")) must(catalog.medicine(id),"药品不存在");else if(kind.equals("item")) must(catalog.item(id),"项目不存在");else throw new BusinessException(400,"目录类型无效");
        if(kind.equals("medicine")) catalog.setMedicineActive(id,req.get("active"));else catalog.setItemActive(id,req.get("active"));auth.audit(actor,"CATALOG_STATUS",id,"更新目录启用状态");return Map.of("ok",true);
    }
    @GetMapping("/admin/audit") Object audit(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpSession s) {auth.require(s);size=size(size);return page(users.auditLogs(size,offset(page,size)),users.countLogs(),page,size);}
}
