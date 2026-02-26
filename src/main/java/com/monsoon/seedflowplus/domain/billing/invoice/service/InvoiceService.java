//package com.monsoon.seedflowplus.domain.billing.invoice.service;
//
//import com.monsoon.seedflowplus.core.common.support.error.CoreException;
//import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
//import com.monsoon.seedflowplus.domain.account.entity.Client;
//import com.monsoon.seedflowplus.domain.account.entity.Employee;
//import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
//import com.monsoon.seedflowplus.domain.billing.invoice.dto.request.InvoiceCreateRequest;
//import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoiceDetailResponse;
//import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoiceListResponse;
//import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoicePublishResponse;
//import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoiceResponse;
//import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
//import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatement;
//import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
//import com.monsoon.seedflowplus.domain.billing.invoice.repository.InvoiceRepository;
//import com.monsoon.seedflowplus.domain.billing.invoice.repository.InvoiceStatementRepository;
//import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
//import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
//import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractHeaderRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class InvoiceService {
//
//    private final InvoiceRepository invoiceRepository;
//    private final InvoiceStatementRepository invoiceStatementRepository;
//    private final ContractHeaderRepository contractHeaderRepository;
//    private final EmployeeRepository employeeRepository;
//
//    /**
//     * 청구서 수동 생성 (영업사원)
//     * - 기간 내 청구 가능한 명세서 자동 조회 후 연결
//     * - 이미 DRAFT/PUBLISHED 청구서가 있으면 생성 불가
//     */
//    @Transactional
//    public InvoiceDetailResponse createInvoice(InvoiceCreateRequest request, Long employeeId) {
//        // 1. 계약 조회
//        ContractHeader contract = contractHeaderRepository.findById(request.getContractId())
//                .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));
//
//        // 2. 중복 청구서 생성 방지 (DRAFT 또는 PUBLISHED 상태가 이미 존재하면 불가)
//        if (invoiceRepository.existsByContractIdAndStatusIn(
//                request.getContractId(),
//                List.of(InvoiceStatus.DRAFT, InvoiceStatus.PUBLISHED))) {
//            throw new CoreException(ErrorType.INVOICE_ALREADY_EXISTS);
//        }
//
//        // 3. 영업사원 / 거래처 조회
//        Employee employee = employeeRepository.findById(employeeId)
//                .orElseThrow(() -> new CoreException(ErrorType.EMPLOYEE_NOT_FOUND));
//        Client client = contract.getClient();
//
//        // 4. 청구서 생성
//        String invoiceCode = generateCode("INV");
//        Invoice invoice = Invoice.create(
//                contract.getId(), client, employee,
//                LocalDate.now(), request.getStartDate(), request.getEndDate(),
//                invoiceCode, request.getMemo()
//        );
//        invoiceRepository.save(invoice);
//
//        // 5. 청구 가능한 명세서 조회 후 InvoiceStatement 연결
//        List<Statement> billableStatements =
//                invoiceStatementRepository.findBillableStatements(contract.getId());
//
//        BigDecimal totalAmount = BigDecimal.ZERO;
//        for (Statement statement : billableStatements) {
//            InvoiceStatement invoiceStatement = InvoiceStatement.create(invoice, statement);
//            invoiceStatementRepository.save(invoiceStatement);
//            totalAmount = totalAmount.add(statement.getTotalAmount());
//        }
//
//        // 6. 청구서 금액 업데이트
//        invoice.updateAmount(totalAmount);
//
//        // 7. 연결된 InvoiceStatement 목록 조회 후 반환
//        List<InvoiceStatement> invoiceStatements =
//                invoiceStatementRepository.findAllByInvoiceId(invoice.getId());
//
//        return InvoiceDetailResponse.of(invoice, invoiceStatements);
//    }
//
//
//    //청구서 발행 확정 (DRAFT → PUBLISHED)
//
//    @Transactional
//    public InvoicePublishResponse publishInvoice(Long invoiceId) {
//        Invoice invoice = invoiceRepository.findById(invoiceId)
//                .orElseThrow(() -> new CoreException(ErrorType.INVOICE_NOT_FOUND));
//
//        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
//            throw new CoreException(ErrorType.INVOICE_ALREADY_PUBLISHED);
//        }
//
//        invoice.publish();
//        return InvoicePublishResponse.from(invoice);
//    }
//
//    /**
//     * 명세서 포함/제외 토글 (영업사원)
//     * - 제외 시 included=false → 다음 청구 주기에 재포함 대상
//     * - 금액 재계산
//     */
//    @Transactional
//    public InvoiceDetailResponse toggleStatement(Long invoiceId, Long statementId) {
//        Invoice invoice = invoiceRepository.findById(invoiceId)
//                .orElseThrow(() -> new CoreException(ErrorType.INVOICE_NOT_FOUND));
//
//        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
//            throw new CoreException(ErrorType.INVOICE_ALREADY_PUBLISHED);
//        }
//
//        InvoiceStatement invoiceStatement = invoiceStatementRepository
//                .findAllByInvoiceId(invoiceId).stream()
//                .filter(is -> is.getStatement().getId().equals(statementId))
//                .findFirst()
//                .orElseThrow(() -> new CoreException(ErrorType.STATEMENT_NOT_FOUND));
//
//        // 포함/제외 토글
//        if (invoiceStatement.isIncluded()) {
//            invoiceStatement.exclude();
//        } else {
//            invoiceStatement.include();
//        }
//
//        // 금액 재계산 (included=true인 것만)
//        List<InvoiceStatement> allStatements =
//                invoiceStatementRepository.findAllByInvoiceId(invoiceId);
//        BigDecimal totalAmount = allStatements.stream()
//                .filter(InvoiceStatement::isIncluded)
//                .map(is -> is.getStatement().getTotalAmount())
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//        invoice.updateAmount(totalAmount);
//
//        return InvoiceDetailResponse.of(invoice, allStatements);
//    }
//
//    /**
//     * 청구서 단건 조회 (공통 - memo 없음)
//     */
//    public InvoiceResponse getInvoice(Long invoiceId) {
//        Invoice invoice = invoiceRepository.findById(invoiceId)
//                .orElseThrow(() -> new CoreException(ErrorType.INVOICE_NOT_FOUND));
//        List<InvoiceStatement> invoiceStatements =
//                invoiceStatementRepository.findAllByInvoiceId(invoiceId);
//        return InvoiceResponse.of(invoice, invoiceStatements);
//    }
//
//    /**
//     * 청구서 단건 조회 (영업사원 전용 - memo 포함)
//     */
//    public InvoiceDetailResponse getInvoiceDetail(Long invoiceId) {
//        Invoice invoice = invoiceRepository.findById(invoiceId)
//                .orElseThrow(() -> new CoreException(ErrorType.INVOICE_NOT_FOUND));
//        List<InvoiceStatement> invoiceStatements =
//                invoiceStatementRepository.findAllByInvoiceId(invoiceId);
//        return InvoiceDetailResponse.of(invoice, invoiceStatements);
//    }
//
//
//    //청구서 목록 조회 (전체)
//
//    public List<InvoiceListResponse> getInvoices() {
//        return invoiceRepository.findAll().stream()
//                .map(InvoiceListResponse::from)
//                .toList();
//    }
//
//
//    // 청구서 목록 조회 (거래처별)
//
//    public List<InvoiceListResponse> getInvoicesByClient(Long clientId) {
//        return invoiceRepository.findAllByClientId(clientId).stream()
//                .map(InvoiceListResponse::from)
//                .toList();
//    }
//
//    /**
//     * 스케줄러용 자동 생성 (billing_cycle 기준)
//     * InvoiceScheduler에서 호출
//     */
//    @Transactional
//    public void createDraftInvoice(ContractHeader contract) {
//        // 이미 DRAFT/PUBLISHED 청구서 존재하면 스킵
//        if (invoiceRepository.existsByContractIdAndStatusIn(
//                contract.getId(),
//                List.of(InvoiceStatus.DRAFT, InvoiceStatus.PUBLISHED))) {
//            return;
//        }
//
//        // 청구 기간 계산 (이번 달 1일 ~ 말일)
//        LocalDate today = LocalDate.now();
//        LocalDate startDate = today.withDayOfMonth(1);
//        LocalDate endDate = today.withDayOfMonth(today.lengthOfMonth());
//
//        String invoiceCode = generateCode("INV");
//        // 스케줄러 자동 생성은 employee 없이 생성 (null 허용 또는 시스템 계정 사용)
//        Invoice invoice = Invoice.create(
//                contract.getId(), contract.getClient(), null,
//                today, startDate, endDate,
//                invoiceCode, null
//        );
//        invoiceRepository.save(invoice);
//
//        // 청구 가능한 명세서 연결
//        List<Statement> billableStatements =
//                invoiceStatementRepository.findBillableStatements(contract.getId());
//
//        BigDecimal totalAmount = BigDecimal.ZERO;
//        for (Statement statement : billableStatements) {
//            invoiceStatementRepository.save(InvoiceStatement.create(invoice, statement));
//            totalAmount = totalAmount.add(statement.getTotalAmount());
//        }
//
//        invoice.updateAmount(totalAmount);
//    }
//
//    // INV-20260223-001 형식으로 코드 생성
//    private String generateCode(String prefix) {
//        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//        String todayPrefix = prefix + "-" + date + "-";
//
//        int nextSeq = invoiceRepository.findMaxSuffixByPrefix(todayPrefix)
//                .map(max -> max + 1)
//                .orElse(1);
//
//        return todayPrefix + String.format("%03d", nextSeq);
//    }
//}