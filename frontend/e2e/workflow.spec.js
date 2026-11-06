import {test,expect} from '@playwright/test';
const password=process.env.E2E_PASSWORD||'His@2026!!';
async function login(page,name){await page.goto('/');await page.getByPlaceholder('输入账号').fill(name);await page.getByPlaceholder('输入密码').fill(password);await page.getByRole('button',{name:/^登录/}).click();await expect(page.locator('.sidebar')).toBeVisible();}
async function logout(page){await page.locator('.account').hover();await page.locator('.el-dropdown-menu:visible .el-dropdown-menu__item').filter({hasText:'退出登录'}).click();await expect(page.getByPlaceholder('输入账号')).toBeVisible();}
async function selectOption(page,field,text){await field.locator('.el-select').click();await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({hasText:text}).first().click();}
async function post(page,path,body,method='POST'){const s=await(await page.request.get('/api/auth/session')).json();const response=await page.request.fetch(`/api${path}`,{method,headers:{'X-CSRF-Token':s.csrf_token},data:body});expect(response.ok(),await response.text()).toBeTruthy();return response.json();}
test('完整页面流程：挂号、病历、检查、收费、结果与确诊',async({page})=>{
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await login(page,'cashier');await page.getByRole('button',{name:'挂号收费',exact:true}).click();
  await page.getByRole('row').filter({hasText:'张示例'}).click();
  const doctorField=page.locator('.el-form-item').filter({hasText:'看诊医生 / 时段'});
  await selectOption(page,doctorField,/李医生/);
  await page.getByRole('button',{name:/收款并完成挂号/}).click();
  await expect(page.getByRole('dialog',{name:'门诊收费凭证'})).toBeVisible();
  await page.getByRole('dialog',{name:'门诊收费凭证'}).getByRole('button',{name:'关闭',exact:true}).click();
  await logout(page);await login(page,'doctor');await page.getByRole('button',{name:'门诊诊疗',exact:true}).click();await page.locator('.patient-row').filter({hasText:'张示例'}).first().click();
  await page.locator('.el-form-item').filter({hasText:'主诉 *'}).locator('textarea').fill('咳嗽三天，前来复诊');
  const initial=page.locator('.el-form-item').filter({hasText:'初步诊断 *'});await initial.locator('input').click();await initial.locator('input').fill('咳嗽');
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({hasText:'咳嗽'}).click();
  await page.getByRole('button',{name:'保存病历',exact:true}).click();await expect(page.getByText('病历已保存',{exact:true})).toBeVisible();
  await page.getByRole('tab',{name:'医技申请',exact:true}).click();
  await selectOption(page,page.locator('.el-form-item').filter({hasText:'选择项目（按编码或名称搜索，可多选）'}),/心电图/);await page.locator('.page-heading').click();
  await page.getByRole('button',{name:'提交申请',exact:true}).click();await expect(page.getByText('申请已提交，等待窗口收费',{exact:true})).toBeVisible();
  await logout(page);await login(page,'cashier');await page.getByRole('button',{name:'挂号收费',exact:true}).click();await page.getByRole('tab',{name:'收费 / 退费 / 退号',exact:true}).click();
  await page.getByRole('row').filter({hasText:'张示例'}).first().getByRole('button',{name:'查看费用'}).click();
  const item=page.getByRole('row').filter({hasText:'心电图'}).first();await item.locator('.el-checkbox').click();await page.getByRole('button',{name:'确认收款',exact:true}).click();
  await expect(page.getByRole('dialog',{name:'门诊收费凭证'})).toBeVisible();await page.getByRole('dialog',{name:'门诊收费凭证'}).getByRole('button',{name:'关闭',exact:true}).click();
  await logout(page);await login(page,'check');await page.getByRole('button',{name:'医技工作站',exact:true}).click();await expect(page.getByRole('row').filter({hasText:'张示例'}).first()).toBeVisible();await page.locator('.el-table__fixed-right').getByRole('button',{name:'分配并登记',exact:true}).click();
  await page.getByRole('dialog',{name:'分配并登记执行'}).getByRole('button',{name:'开始检查',exact:true}).click();await page.locator('.el-table__fixed-right').getByRole('button',{name:'录入结果',exact:true}).click();
  await page.getByPlaceholder('填写结果内容，核对后提交').fill('窦性心律，未见明显异常。');await page.getByRole('button',{name:'提交结果',exact:true}).click();await expect(page.getByText('结果已回传医生工作站',{exact:true})).toBeVisible();
  await logout(page);await login(page,'doctor');await page.getByRole('button',{name:'门诊诊疗',exact:true}).click();await page.locator('.patient-row').filter({hasText:'张示例'}).first().click();await page.getByRole('tab',{name:'结果与费用',exact:true}).click();await expect(page.locator('.result-report')).toContainText('窦性心律');
  await page.getByRole('tab',{name:'门诊确诊',exact:true}).click();const diagnosis=page.locator('.el-form-item').filter({hasText:'确诊疾病 *'});await diagnosis.locator('input').click();await diagnosis.locator('input').fill('急性上呼吸道感染');await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({hasText:'急性上呼吸道感染'}).first().click();await page.getByRole('button',{name:'提交确诊',exact:true}).click();await expect(page.getByText('确诊已提交',{exact:true})).toBeVisible();
  expect(errors).toEqual([]);
});
test('管理员与药房页面可用，窄屏登录可操作',async({page})=>{
  const errors=[];page.on('pageerror',e=>errors.push(e.message));await login(page,'admin');await page.getByRole('button',{name:'系统管理',exact:true}).click();
  for(const tab of ['医生排班','科室目录','项目与药品','操作审计']){await page.getByRole('tab',{name:tab,exact:true}).click();await expect(page.locator('.el-tabs__content')).toBeVisible();}
  await logout(page);await login(page,'pharmacy');await page.getByRole('button',{name:'药房管理',exact:true}).click();await page.getByRole('tab',{name:'药库查询 / 入库',exact:true}).click();await expect(page.getByRole('button',{name:'药品入库',exact:true})).toBeVisible();await page.getByRole('tab',{name:'药品交易记录',exact:true}).click();await logout(page);
  await page.setViewportSize({width:390,height:844});await expect(page.getByPlaceholder('输入账号')).toBeVisible();expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1)).toBeTruthy();expect(errors).toEqual([]);
});
test('处方页面、药品入库、收费、发药退药与退款闭环',async({page})=>{
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await login(page,'cashier');
  const patient=await post(page,'/patients',{name:`药房验收${Date.now()}`,gender:'女',settle_type:'自费'});
  const schedules=await(await page.request.get('/api/schedules')).json();const schedule=schedules.find(s=>s.doctor_name==='李医生');
  const registration=await post(page,'/registrations',{patient_id:patient.id,schedule_id:schedule.id,book_requested:false,pay_method:'移动支付',received_cents:schedule.fee_cents,request_key:crypto.randomUUID()});
  const encounter=registration.encounter;
  await logout(page);await login(page,'doctor');
  await post(page,`/encounters/${encounter.id}/record`,{chief_complaint:'处方页面验收',initial_diagnosis:'营养评估',version:0},'PUT');
  await post(page,`/encounters/${encounter.id}/diagnosis`,{disease_name:'营养评估',opinion:'按医嘱处理'});
  await page.getByRole('button',{name:'门诊诊疗',exact:true}).click();
  await page.getByPlaceholder('姓名 / 病历号').fill(patient.case_no);await page.getByPlaceholder('姓名 / 病历号').press('Enter');
  await page.locator('.patient-row').filter({hasText:patient.name}).click();await page.getByRole('tab',{name:'开立处方',exact:true}).click();
  await selectOption(page,page.getByRole('tabpanel',{name:'开立处方'}).locator('.toolbar'),/维生素C片/);
  await page.getByRole('button',{name:'添加药品',exact:true}).click();
  await page.locator('.drug-editor .el-form-item').filter({hasText:'数量'}).locator('input').fill('2');
  await page.getByPlaceholder('如：口服').fill('口服');await page.getByPlaceholder('如：每次1片').fill('每次1片');await page.getByPlaceholder('如：每日两次').fill('每日两次');
  await page.getByRole('button',{name:'开立处方',exact:true}).click();await expect(page.getByText('处方已开立',{exact:true})).toBeVisible();
  await logout(page);await login(page,'cashier');await page.getByRole('button',{name:'挂号收费',exact:true}).click();await page.getByRole('tab',{name:'收费 / 退费 / 退号',exact:true}).click();
  await page.getByPlaceholder('输入姓名、病历号或就诊号').fill(patient.case_no);await page.getByPlaceholder('输入姓名、病历号或就诊号').press('Enter');
  await page.getByRole('row').filter({hasText:patient.name}).getByRole('button',{name:'查看费用'}).click();
  await page.getByRole('row').filter({hasText:'维生素C片'}).first().locator('.el-checkbox').click();await page.getByRole('button',{name:'确认收款',exact:true}).click();
  await expect(page.getByRole('dialog',{name:'门诊收费凭证'})).toBeVisible();await page.getByRole('dialog',{name:'门诊收费凭证'}).getByRole('button',{name:'关闭',exact:true}).click();
  await logout(page);await login(page,'pharmacy');await page.getByRole('button',{name:'药房管理',exact:true}).click();await page.getByRole('tab',{name:'药库查询 / 入库',exact:true}).click();
  await page.getByRole('button',{name:'药品入库',exact:true}).click();const stockDialog=page.getByRole('dialog',{name:'药品入库'});
  await selectOption(page,stockDialog.locator('.el-form-item').filter({hasText:'药品'}),/维生素C片/);
  await stockDialog.locator('.el-form-item').filter({hasText:'入库数量'}).locator('input').fill('10');await page.getByPlaceholder('如：采购验收入库单号').fill('UI-QA-入库');
  await stockDialog.getByRole('button',{name:'确认入库',exact:true}).click();await expect(page.getByText('库存已增加，入库流水已保存',{exact:true})).toBeVisible();
  await page.getByRole('tab',{name:'发药 / 退药',exact:true}).click();await page.getByPlaceholder('患者姓名 / 病历号',{exact:true}).fill(patient.case_no);await page.getByPlaceholder('患者姓名 / 病历号',{exact:true}).press('Enter');
  await page.locator('.el-table__fixed-right').getByRole('button',{name:'核对并发药',exact:true}).click();await page.locator('.el-message-box').getByRole('button',{name:'确定',exact:true}).click();await expect(page.getByText('发药成功，库存和交易记录已更新',{exact:true})).toBeVisible();
  await page.locator('.short-select').click();await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({hasText:'已发（当前页）'}).click();
  await page.locator('.el-table__fixed-right').getByRole('button',{name:'退回药品',exact:true}).click();await page.locator('.el-message-box__input input').fill('药品完整退回');await page.locator('.el-message-box').getByRole('button',{name:'确认',exact:true}).click();
  await expect(page.getByText('退药已登记，请到收费窗口办理退费',{exact:true})).toBeVisible();
  await logout(page);await login(page,'cashier');await page.getByRole('button',{name:'挂号收费',exact:true}).click();await page.getByRole('tab',{name:'收费 / 退费 / 退号',exact:true}).click();
  await page.getByPlaceholder('输入姓名、病历号或就诊号').fill(patient.case_no);await page.getByPlaceholder('输入姓名、病历号或就诊号').press('Enter');await page.getByRole('row').filter({hasText:patient.name}).getByRole('button',{name:'查看费用'}).click();
  await page.getByRole('row').filter({hasText:'维生素C片'}).first().getByRole('button',{name:'退费',exact:true}).click();await page.locator('.el-message-box__input input').fill('已确认药房退回');await page.locator('.el-message-box').getByRole('button',{name:'确认',exact:true}).click();await expect(page.getByText('退款已记录',{exact:true})).toBeVisible();
  const detail=await(await page.request.get(`/api/encounters/${encounter.id}`)).json();expect(detail.orders.find(o=>o.kind==='DRUG').billing_status).toBe('REFUNDED');expect(detail.refunds).toHaveLength(1);
  const catalog=await(await page.request.get('/api/catalog')).json();expect(catalog.medicines.find(m=>m.code==='M001').stock).toBe(10);expect(errors).toEqual([]);
});
