package srm.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "srm.db")
public class SrmDbProperties {
    private Map<String, String> pkColumns = new HashMap<>();
    private String userDetailFilterColumn;
    private String bladeUserSupplierFilterColumn = "supplier_id";
    private String supplierIdFromUserDetailColumn = "SUPPLIER_COMPANY_ID";
    private String purchaseOrderSupplierFilterColumn = "supplier_id";
    private String invoiceOrderFilterColumn = "order_id";
    private String invoiceOrderIdColumn = "PUCHASE_ORDER_ID";
    private String purchaseOrderNoColumn = "order_no";
    private String invoiceSalesOrderNoColumn = "SALES_ORDER_NO";
    private String authAccountColumn = "account";
    private String authPasswordColumn = "password";
    private int defaultPageSize = 20;

    public Map<String, String> getPkColumns() {
        return pkColumns;
    }

    public void setPkColumns(Map<String, String> pkColumns) {
        this.pkColumns = pkColumns;
    }

    public String getUserDetailFilterColumn() {
        return userDetailFilterColumn;
    }

    public void setUserDetailFilterColumn(String userDetailFilterColumn) {
        this.userDetailFilterColumn = userDetailFilterColumn;
    }

    public String getSupplierIdFromUserDetailColumn() {
        return supplierIdFromUserDetailColumn;
    }

    public void setSupplierIdFromUserDetailColumn(String supplierIdFromUserDetailColumn) {
        this.supplierIdFromUserDetailColumn = supplierIdFromUserDetailColumn;
    }

    public String getBladeUserSupplierFilterColumn() {
        return bladeUserSupplierFilterColumn;
    }

    public void setBladeUserSupplierFilterColumn(String bladeUserSupplierFilterColumn) {
        this.bladeUserSupplierFilterColumn = bladeUserSupplierFilterColumn;
    }

    public String getPurchaseOrderSupplierFilterColumn() {
        return purchaseOrderSupplierFilterColumn;
    }

    public void setPurchaseOrderSupplierFilterColumn(String purchaseOrderSupplierFilterColumn) {
        this.purchaseOrderSupplierFilterColumn = purchaseOrderSupplierFilterColumn;
    }

    public String getInvoiceOrderFilterColumn() {
        return invoiceOrderFilterColumn;
    }

    public void setInvoiceOrderFilterColumn(String invoiceOrderFilterColumn) {
        this.invoiceOrderFilterColumn = invoiceOrderFilterColumn;
    }

    public String getInvoiceOrderIdColumn() {
        return invoiceOrderIdColumn;
    }

    public void setInvoiceOrderIdColumn(String invoiceOrderIdColumn) {
        this.invoiceOrderIdColumn = invoiceOrderIdColumn;
    }

    public String getPurchaseOrderNoColumn() {
        return purchaseOrderNoColumn;
    }

    public void setPurchaseOrderNoColumn(String purchaseOrderNoColumn) {
        this.purchaseOrderNoColumn = purchaseOrderNoColumn;
    }

    public String getInvoiceSalesOrderNoColumn() {
        return invoiceSalesOrderNoColumn;
    }

    public void setInvoiceSalesOrderNoColumn(String invoiceSalesOrderNoColumn) {
        this.invoiceSalesOrderNoColumn = invoiceSalesOrderNoColumn;
    }

    public String getAuthAccountColumn() {
        return authAccountColumn;
    }

    public void setAuthAccountColumn(String authAccountColumn) {
        this.authAccountColumn = authAccountColumn;
    }

    public String getAuthPasswordColumn() {
        return authPasswordColumn;
    }

    public void setAuthPasswordColumn(String authPasswordColumn) {
        this.authPasswordColumn = authPasswordColumn;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }
}

