package srm.portal.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import srm.portal.repository.OrderCollaborationRepository;
import srm.portal.repository.GenericTableLookupRepository;

import java.util.Optional;

@Service
public class supplierOrderStatusService {

    @Autowired
    private OrderCollaborationRepository orderRepo;

    @Autowired
    private GenericTableLookupRepository lookupRepo;

    /**
     * 订单状态常量
     */
    public static final int STATUS_PENDING_CONFIRMATION = 0;    // 待确认
    public static final int STATUS_CONFIRMED = 20;              // 已确认
    public static final int STATUS_PARTIAL_SHIPPED = 25;        // 部分发运
    public static final int STATUS_SHIPPED = 30;                 // 已发运
    public static final int STATUS_CLOSED = 40;                    // 已结案

    /**
     * 供应商确认订单（标准 PO）
     * 状态流转：待反馈 → 已确认
     */
    @Transactional
    public void supplierConfirmOrder(Long orderId, Long userId, String reason) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            throw new RuntimeException("当前用户未绑定供应商，无法确认订单");
        }

        // 检查订单是否属于该供应商
        Optional<Long> orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId);
        if (orderSupplierId.isEmpty() || !orderSupplierId.get().equals(supplierId.get())) {
            throw new RuntimeException("该订单不属于当前供应商");
        }

        // 更新订单状态为已确认
        String actionLog = buildActionLog("供应商确认订单", userId, reason);
        int affected = orderRepo.updatesupplierOrderStatusForSupplier(
                orderId, 
                supplierId.get(), 
                STATUS_CONFIRMED, 
                userId, 
                actionLog
        );

        if (affected == 0) {
            throw new RuntimeException("订单确认失败，订单状态可能已变更");
        }

        System.out.println("订单确认成功，orderId: " + orderId + ", userId: " + userId);
    }

    /**
     * 供应商拒绝订单（标准 PO）
     * 状态流转：待反馈 → 待反馈（记录拒绝原因）
     */
    @Transactional
    public void supplierRejectOrder(Long orderId, Long userId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("拒绝原因不能为空");
        }

        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            throw new RuntimeException("当前用户未绑定供应商，无法拒绝订单");
        }

        // 检查订单是否属于该供应商
        Optional<Long> orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId);
        if (orderSupplierId.isEmpty() || !orderSupplierId.get().equals(supplierId.get())) {
            throw new RuntimeException("该订单不属于当前供应商");
        }

        // 拒绝订单，状态保持为待确认，但记录拒绝原因
        String actionLog = buildActionLog("供应商拒绝订单", userId, reason);
        int affected = orderRepo.updatesupplierOrderStatusForSupplier(
                orderId, 
                supplierId.get(), 
                STATUS_PENDING_CONFIRMATION, 
                userId, 
                actionLog
        );

        if (affected == 0) {
            throw new RuntimeException("订单拒绝失败，订单状态可能已变更");
        }

        System.out.println("订单拒绝成功，orderId: " + orderId + ", userId: " + userId + ", reason: " + reason);
    }

    /**
     * 订单部分发运
     * 状态流转：已确认 → 部分发运
     */
    @Transactional
    public void orderPartialShipped(Long orderId, Long userId) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            throw new RuntimeException("当前用户未绑定供应商");
        }

        // 检查订单是否属于该供应商
        Optional<Long> orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId);
        if (orderSupplierId.isEmpty() || !orderSupplierId.get().equals(supplierId.get())) {
            throw new RuntimeException("该订单不属于当前供应商");
        }

        // 更新订单状态为部分发运
        String actionLog = buildActionLog("订单部分发运", userId, "");
        int affected = orderRepo.updatesupplierOrderStatusForSupplier(
                orderId, 
                supplierId.get(), 
                STATUS_PARTIAL_SHIPPED, 
                userId, 
                actionLog
        );

        if (affected == 0) {
            throw new RuntimeException("订单状态更新失败");
        }

        System.out.println("订单部分发运状态更新成功，orderId: " + orderId);
    }

    /**
     * 订单全部发运
     * 状态流转：已确认/部分发运 → 已发运
     */
    @Transactional
    public void orderFullyShipped(Long orderId, Long userId) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            throw new RuntimeException("当前用户未绑定供应商");
        }

        // 检查订单是否属于该供应商
        Optional<Long> orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId);
        if (orderSupplierId.isEmpty() || !orderSupplierId.get().equals(supplierId.get())) {
            throw new RuntimeException("该订单不属于当前供应商");
        }

        // 更新订单状态为已发运
        String actionLog = buildActionLog("订单全部发运", userId, "");
        int affected = orderRepo.updatesupplierOrderStatusForSupplier(
                orderId, 
                supplierId.get(), 
                STATUS_SHIPPED, 
                userId, 
                actionLog
        );

        if (affected == 0) {
            throw new RuntimeException("订单状态更新失败");
        }

        System.out.println("订单全部发运状态更新成功，orderId: " + orderId);
    }

    /**
     * 订单结案
     * 状态流转：已发运 → 已结案
     */
    @Transactional
    public void orderClosed(Long orderId, Long userId, String reason) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            throw new RuntimeException("当前用户未绑定供应商");
        }

        // 检查订单是否属于该供应商
        Optional<Long> orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId);
        if (orderSupplierId.isEmpty() || !orderSupplierId.get().equals(supplierId.get())) {
            throw new RuntimeException("该订单不属于当前供应商");
        }

        // 更新订单状态为已结案
        String actionLog = buildActionLog("订单结案", userId, reason);
        int affected = orderRepo.updatesupplierOrderStatusForSupplier(
                orderId, 
                supplierId.get(), 
                STATUS_CLOSED, 
                userId, 
                actionLog
        );

        if (affected == 0) {
            throw new RuntimeException("订单状态更新失败");
        }

        System.out.println("订单结案成功，orderId: " + orderId);
    }

    /**
     * JIT 订单自动确认
     * JIT 订单接收后状态直接默认为已确认
     */
    @Transactional
    public void jitOrderAutoConfirm(Long orderId) {
        // JIT 订单自动确认，不需要用户操作
        String actionLog = buildActionLog("JIT订单自动确认", 0L, "系统自动确认");
        int affected = orderRepo.updatesupplierOrderStatusForSupplier(
                orderId, 
                0L, 
                STATUS_CONFIRMED, 
                0L, 
                actionLog
        );

        if (affected == 0) {
            System.out.println("JIT订单自动确认失败，orderId: " + orderId);
        } else {
            System.out.println("JIT订单自动确认成功，orderId: " + orderId);
        }
    }

    /**
     * 供应商确认订单关闭
     * 状态流转：待确认关闭 → 已关闭
     */
    @Transactional
    public void supplierConfirmOrderClose(Long orderId, Long userId, String reason) {
        Optional<Long> supplierId = lookupRepo.findSupplierCompanyIdByUserId(String.valueOf(userId));
        if (supplierId.isEmpty()) {
            throw new RuntimeException("当前用户未绑定供应商，无法确认订单关闭");
        }

        // 检查订单是否属于该供应商
        Optional<Long> orderSupplierId = orderRepo.findSupplierIdByOrderId(orderId);
        if (orderSupplierId.isEmpty() || !orderSupplierId.get().equals(supplierId.get())) {
            throw new RuntimeException("该订单不属于当前供应商");
        }

        // 更新订单状态为已结案
        String actionLog = buildActionLog("供应商确认订单关闭", userId, reason);
        int affected = orderRepo.updatesupplierOrderStatusForSupplier(
                orderId, 
                supplierId.get(), 
                STATUS_CLOSED, 
                userId, 
                actionLog
        );

        if (affected == 0) {
            throw new RuntimeException("订单关闭确认失败");
        }

        System.out.println("订单关闭确认成功，orderId: " + orderId + ", userId: " + userId);
    }

    /**
     * 获取订单状态文本
     */
    public static String getStatusText(int status) {
        switch (status) {
            case STATUS_PENDING_CONFIRMATION:
                return "待确认";
            case STATUS_CONFIRMED:
                return "已确认";
            case STATUS_PARTIAL_SHIPPED:
                return "部分发运";
            case STATUS_SHIPPED:
                return "已发运";
            case STATUS_CLOSED:
                return "已结案";
            default:
                return "未知状态";
        }
    }

    /**
     * 获取订单状态颜色
     */
    public static String getStatusColor(int status) {
        switch (status) {
            case STATUS_PENDING_CONFIRMATION:
                return "orange";
            case STATUS_CONFIRMED:
                return "green";
            case STATUS_PARTIAL_SHIPPED:
                return "blue";
            case STATUS_SHIPPED:
                return "cyan";
            case STATUS_CLOSED:
                return "gray";
            default:
                return "default";
        }
    }

    /**
     * 检查订单状态是否可以发运
     */
    public static boolean canShip(int status) {
        return status == STATUS_CONFIRMED || status == STATUS_PARTIAL_SHIPPED;
    }

    /**
     * 检查订单状态是否可以确认
     */
    public static boolean canConfirm(int status) {
        return status == STATUS_PENDING_CONFIRMATION;
    }

    /**
     * 检查订单状态是否可以拒绝
     */
    public static boolean canReject(int status) {
        return status == STATUS_PENDING_CONFIRMATION;
    }

    /**
     * 构建操作日志
     */
    private String buildActionLog(String action, Long userId, String reason) {
        String safeReason = (reason == null || reason.isBlank()) ? "-" : reason.trim();
        return "\n[SRM-" + action + "] time=" + java.time.LocalDateTime.now() + 
               ", userId=" + userId + ", reason=" + safeReason;
    }
}