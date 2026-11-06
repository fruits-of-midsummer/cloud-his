package com.cloudhis.config;

import com.cloudhis.mapper.*;
import com.cloudhis.service.AuthService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.cloudhis.common.Values.*;

@Component
public class SeedData implements ApplicationRunner {
    private final AuthMapper users;private final CatalogMapper catalog;private final EncounterMapper encounters;private final AuthService auth;
    @Value("${his.seed}") private boolean seed;
    @Value("${his.initial-password}") private String password;
    @Value("${his.production}") private boolean production;
    public SeedData(AuthMapper u,CatalogMapper c,EncounterMapper e,AuthService a){users=u;catalog=c;encounters=e;auth=a;}
    @Override @Transactional public void run(ApplicationArguments args) {
        if(password.length()<10 || (production && password.equals("His@2026!!"))) throw new IllegalStateException("请通过 HIS_INITIAL_PASSWORD 设置至少10位的独立初始密码");
        if(users.byUsername("admin")==null) addUser("admin","系统管理员","ADMIN",null);
        if(!seed) return;
        if(catalog.departmentCount()==0) {
            for(String[] d:List.of(new String[]{"general","QK","全科门诊","OUTPATIENT"},new String[]{"internal","NK","内科门诊","OUTPATIENT"},new String[]{"check","JC","检查科","CHECK"},new String[]{"lab","JY","检验科","LAB"},new String[]{"treat","CZ","处置科","TREAT"},new String[]{"pharmacy","YF","门诊药房","PHARMACY"})) catalog.addDepartment(map("id",d[0],"code",d[1],"name",d[2],"kind",d[3]));
            addUser("cashier","窗口收费员","CASHIER",null);addUser("doctor","李医生","DOCTOR","general");addUser("doctor2","王医生","DOCTOR","internal");addUser("check","检查医生","CHECK","check");addUser("lab","检验医生","LAB","lab");addUser("treat","处置医生","TREAT","treat");addUser("pharmacy","药房药师","PHARMACY","pharmacy");
            for(Object[] item:List.of(new Object[]{"ECG","心电图","CHECK","常规十二导联",3500L,"check"},new Object[]{"US","腹部超声","CHECK","肝胆胰脾",12000L,"check"},new Object[]{"CBC","血常规","LAB","静脉血",2500L,"lab"},new Object[]{"GLU","空腹血糖","LAB","血清",1200L,"lab"},new Object[]{"NEB","雾化吸入","TREAT","一次",3000L,"treat"},new Object[]{"DRESS","伤口换药","TREAT","小换药",2000L,"treat"})) catalog.addItem(map("id",id(),"code",item[0],"name",item[1],"kind",item[2],"specification",item[3],"price_cents",item[4],"department_id",item[5]));
            for(Object[] drug:List.of(new Object[]{"M001","维生素C片","wssc",1200L},new Object[]{"M002","氯化钠注射液","lhNZ",850L},new Object[]{"M003","对乙酰氨基酚片","dyxajf",1850L})) {
                String medicineId=id();catalog.addMedicine(map("id",medicineId,"code",drug[0],"name",drug[1],"pinyin",drug[2],"unit","盒","manufacturer","示例制药企业","formulation","常规剂型","price_cents",drug[3]));
                // 首次库存为0；通过药房入库接口增加库存并留下流水。
            }
            for(String[] disease:List.of(new String[]{"J06.900","急性上呼吸道感染"},new String[]{"I10.x00","原发性高血压"},new String[]{"R05.x00","咳嗽"},new String[]{"E11.900","2型糖尿病"})) catalog.addDisease(disease[0],disease[1]);
            encounters.addPatient(map("id",id(),"case_no",number("P"),"name","张示例","gender","女","birth_date",LocalDate.of(1988,4,12),"settle_type","自费","phone","13800000001"));
            encounters.addPatient(map("id",id(),"case_no",number("P"),"name","陈示例","gender","男","birth_date",LocalDate.of(1975,11,8),"settle_type","自费","phone","13800000002"));
        }
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        Set<String> existing=new HashSet<>();for(Map<String,Object> s:catalog.schedules(today.toString(),today.plusDays(6).toString())) existing.add(str(s,"doctor_id")+str(s,"visit_date")+str(s,"period"));
        for(String username:List.of("doctor","doctor2")) {
            Map<String,Object> doctor=users.byUsername(username);if(doctor==null) continue;
            for(int day=0;day<7;day++) for(String period:List.of("上午","下午")) {
                LocalDate date=today.plusDays(day);String key=str(doctor,"id")+date+period;
                if(!existing.contains(key)) catalog.addSchedule(map("id",id(),"doctor_id",str(doctor,"id"),"visit_date",date,"period",period,"level_name","普通号","fee_cents",2000L,"quota",30));
            }
        }
    }
    private void addUser(String name,String display,String role,String dept) {users.insert(map("id",id(),"username",name,"password_hash",auth.encode(password),"display_name",display,"role_code",role,"department_id",dept));}
}
