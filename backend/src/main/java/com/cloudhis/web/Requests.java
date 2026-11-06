package com.cloudhis.web;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

public final class Requests {
    private Requests() {}
    public record Login(@NotBlank(message="请输入账号") @Size(max=40) String username, @NotBlank(message="请输入密码") @Size(max=100) String password) {}
    public record Password(@NotBlank String oldPassword, @Size(min=10,max=100,message="新密码长度应为10至100位") @NotBlank String newPassword) {}
    public record Patient(@NotBlank(message="姓名不能为空") @Size(max=60) String name, @Pattern(regexp="男|女|未知",message="请选择性别") @NotBlank String gender,
        @PastOrPresent(message="出生日期不能晚于今天") LocalDate birthDate, @Min(0) @Max(150) Integer age,
        @NotBlank(message="请选择结算类别") @Size(max=20) String settleType, @Size(max=32) String idCard, @Size(max=200) String address, @Size(max=24) String phone) {}
    public record Registration(@NotBlank String patientId, @NotBlank String scheduleId, boolean bookRequested,
        @NotBlank String payMethod, @PositiveOrZero long receivedCents, @NotBlank @Size(max=80) String requestKey) {}
    public record Record(@NotBlank(message="请填写主诉") @Size(max=2000) String chiefComplaint, @Size(max=4000) String presentIllness,
        @Size(max=2000) String treatmentHistory, @Size(max=2000) String pastHistory, @Size(max=2000) String allergyHistory,
        @Size(max=2000) String physicalExam, @NotBlank(message="请填写初步诊断") @Size(max=1000) String initialDiagnosis,
        @Size(max=2000) String suggestions, @PositiveOrZero int version) {}
    public record Diagnosis(@Size(max=32) String diseaseCode, @NotBlank(message="请填写确诊疾病") @Size(max=200) String diseaseName, @Size(max=2000) String opinion) {}
    public record Order(@NotBlank String encounterId, @NotEmpty(message="请至少选择一个项目") @Size(max=50) List<String> itemIds,
        @Size(max=1000) String purpose, @Size(max=100) String bodyPart, @Size(max=1000) String notes) {}
    public record Drug(@NotBlank String medicineId, @Min(1) @Max(10000) int quantity, @NotBlank @Size(max=80) String drugUsage,
        @NotBlank @Size(max=80) String dosage, @NotBlank @Size(max=80) String frequency, @Min(1) @Max(365) int days, @Size(max=1000) String notes) {}
    public record Prescription(@NotBlank String encounterId, @NotEmpty @Size(max=50) List<@Valid Drug> items) {}
    public record Charge(@NotBlank String encounterId, @NotEmpty @Size(max=100) List<String> orderIds,
        @NotBlank String payMethod, @PositiveOrZero long receivedCents, @NotBlank @Size(max=80) String requestKey) {}
    public record Reason(@NotBlank(message="请填写操作原因") @Size(max=500) String reason) {}
    public record Execute(@NotBlank String executorId) {}
    public record Result(@NotBlank(message="请填写执行结果") @Size(max=8000) String content) {}
    public record Stock(@NotBlank String medicineId, @Min(1) @Max(1000000) int quantity, @NotBlank @Size(max=500) String reason) {}
    public record User(@NotBlank @Pattern(regexp="[a-zA-Z][a-zA-Z0-9_]{2,39}") String username,
        @NotBlank @Size(max=60) String displayName, @NotBlank String roleCode, String departmentId,
        @NotBlank @Size(min=10,max=100) String password) {}
    public record Schedule(@NotBlank String doctorId, @NotNull @FutureOrPresent LocalDate visitDate,
        @Pattern(regexp="上午|下午") @NotBlank String period, @NotBlank @Size(max=20) String levelName,
        @Min(0) @Max(10000000) long feeCents, @Min(1) @Max(1000) int quota) {}
    public record ServiceItem(@NotBlank @Size(max=32) String code, @NotBlank @Size(max=100) String name,
        @Pattern(regexp="CHECK|LAB|TREAT") @NotBlank String kind, @Size(max=100) String specification,
        @Min(1) @Max(100000000) long priceCents, @NotBlank String departmentId) {}
    public record Medicine(@NotBlank @Size(max=32) String code, @NotBlank @Size(max=100) String name,
        @Size(max=80) String pinyin, @NotBlank @Size(max=20) String unit, @Size(max=120) String manufacturer,
        @Size(max=40) String formulation, @Min(1) @Max(100000000) long priceCents) {}
    public record Department(@NotBlank @Size(max=32) String code,@NotBlank @Size(max=80) String name,
        @NotBlank @Pattern(regexp="OUTPATIENT|CHECK|LAB|TREAT|PHARMACY") String kind) {}
}
