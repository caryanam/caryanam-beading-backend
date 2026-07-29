package com.bidding.service;

import com.bidding.entity.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfGeneratorService {

    public byte[] generateInspectionPdf(Inspection inspection, 
                                        List<InspectionPanel> panels,
                                        MechanicalInspection mechanical, 
                                        TyreInspection tyres, 
                                        InteriorInspection interior, 
                                        List<InspectionImage> images,
                                        InspectionRemarks remarks) {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Header Section
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD);
            Paragraph title = new Paragraph("VEHICLE INSPECTION REPORT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Vehicle Specifications Table
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD);
            Paragraph vehicleSec = new Paragraph("1. VEHICLE SPECIFICATION", sectionFont);
            vehicleSec.setSpacingAfter(10);
            document.add(vehicleSec);

            PdfPTable vehTable = new PdfPTable(2);
            vehTable.setWidthPercentage(100);
            vehTable.setSpacingAfter(20);

            Vehicle v = inspection.getVehicle();
            if (v != null) {
                addTableCell(vehTable, "Vehicle Number", v.getVehicleNumber());
                addTableCell(vehTable, "Owner Name", v.getOwnerName());
                addTableCell(vehTable, "Brand & Model", v.getBrand() + " " + v.getModel());
                addTableCell(vehTable, "Variant", v.getVariant());
                addTableCell(vehTable, "Manufacturing Year", String.valueOf(v.getManufacturingYear()));
                addTableCell(vehTable, "Fuel Type", v.getFuelType());
                addTableCell(vehTable, "Transmission", v.getTransmission());
                addTableCell(vehTable, "Odometer Reading", v.getOdometerReading() != null ? v.getOdometerReading() + " km" : "N/A");
                addTableCell(vehTable, "Insurance Status", v.getInsuranceStatus());
                addTableCell(vehTable, "Inspection Date", v.getInspectionDate() != null ? v.getInspectionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A");
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No vehicle specifications saved yet."));
                cell.setColspan(2);
                vehTable.addCell(cell);
            }
            document.add(vehTable);

            // Ratings Table
            Paragraph ratingsSec = new Paragraph("SUMMARY RATINGS", sectionFont);
            ratingsSec.setSpacingAfter(10);
            document.add(ratingsSec);

            PdfPTable ratingsTable = new PdfPTable(4);
            ratingsTable.setWidthPercentage(100);
            ratingsTable.setSpacingAfter(20);

            ratingsTable.addCell(new Phrase("Exterior Rating"));
            ratingsTable.addCell(new Phrase(inspection.getExteriorRating() != null ? inspection.getExteriorRating() + "/5" : "N/A"));
            ratingsTable.addCell(new Phrase("Mechanical Rating"));
            ratingsTable.addCell(new Phrase(inspection.getMechanicalRating() != null ? inspection.getMechanicalRating() + "/5" : "N/A"));
            ratingsTable.addCell(new Phrase("Tyre Rating"));
            ratingsTable.addCell(new Phrase(inspection.getTyreRating() != null ? inspection.getTyreRating() + "/5" : "N/A"));
            ratingsTable.addCell(new Phrase("Interior Rating"));
            ratingsTable.addCell(new Phrase(inspection.getInteriorRating() != null ? inspection.getInteriorRating() + "/5" : "N/A"));
            document.add(ratingsTable);

            // 31 Panel Inspection
            Paragraph panelSec = new Paragraph("2. EXTERIOR PANEL INSPECTION", sectionFont);
            panelSec.setSpacingAfter(10);
            document.add(panelSec);

            PdfPTable panelTable = new PdfPTable(3);
            panelTable.setWidthPercentage(100);
            panelTable.setSpacingAfter(20);
            panelTable.addCell(new Phrase("Panel Name"));
            panelTable.addCell(new Phrase("Condition"));
            panelTable.addCell(new Phrase(""));

            if (panels != null && !panels.isEmpty()) {
                for (InspectionPanel p : panels) {
                    panelTable.addCell(new Phrase(p.getPanelName()));
                    panelTable.addCell(new Phrase(p.getCondition().name()));
                    panelTable.addCell(new Phrase(""));
                }
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No panels saved yet."));
                cell.setColspan(3);
                panelTable.addCell(cell);
            }
            document.add(panelTable);

            // Mechanical Check Section
            Paragraph mechSec = new Paragraph("3. MECHANICAL INSPECTION", sectionFont);
            mechSec.setSpacingAfter(10);
            document.add(mechSec);

            PdfPTable mechTable = new PdfPTable(2);
            mechTable.setWidthPercentage(100);
            mechTable.setSpacingAfter(20);

            if (mechanical != null) {
                addTableCell(mechTable, "Engine Status", mechanical.getEngineStatus());
                addTableCell(mechTable, "Engine Oil", mechanical.getEngineOil());
                addTableCell(mechTable, "Brake Oil", mechanical.getBrakeOil());
                addTableCell(mechTable, "Steering Oil", mechanical.getSteeringOil());
                addTableCell(mechTable, "Coolant", mechanical.getCoolant());
                addTableCell(mechTable, "Brake Booster", mechanical.getBrakeBooster());
                addTableCell(mechTable, "Brake Working", mechanical.getBrakeWorking());
                addTableCell(mechTable, "Suspension", mechanical.getSuspension());
                addTableCell(mechTable, "Fluid Leakage", mechanical.getFluidLeakage());
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No mechanical checks saved yet."));
                cell.setColspan(2);
                mechTable.addCell(cell);
            }
            document.add(mechTable);

            // Tyre & Emergency Section
            Paragraph tyreSec = new Paragraph("4. TYRES & EMERGENCY CHECKLIST", sectionFont);
            tyreSec.setSpacingAfter(10);
            document.add(tyreSec);

            PdfPTable tyreTable = new PdfPTable(3);
            tyreTable.setWidthPercentage(100);
            tyreTable.setSpacingAfter(20);
            tyreTable.addCell(new Phrase("Tyre Position"));
            tyreTable.addCell(new Phrase("Brand"));
            tyreTable.addCell(new Phrase("Tread % / Mfg Year"));

            if (tyres != null) {
                tyreTable.addCell(new Phrase("Front Left"));
                tyreTable.addCell(new Phrase(tyres.getFrontLeftBrand()));
                tyreTable.addCell(new Phrase(tyres.getFrontLeftTread() + "% / " + tyres.getFrontLeftYear()));

                tyreTable.addCell(new Phrase("Front Right"));
                tyreTable.addCell(new Phrase(tyres.getFrontRightBrand()));
                tyreTable.addCell(new Phrase(tyres.getFrontRightTread() + "% / " + tyres.getFrontRightYear()));

                tyreTable.addCell(new Phrase("Rear Left"));
                tyreTable.addCell(new Phrase(tyres.getRearLeftBrand()));
                tyreTable.addCell(new Phrase(tyres.getRearLeftTread() + "% / " + tyres.getRearLeftYear()));

                tyreTable.addCell(new Phrase("Rear Right"));
                tyreTable.addCell(new Phrase(tyres.getRearRightBrand()));
                tyreTable.addCell(new Phrase(tyres.getRearRightTread() + "% / " + tyres.getRearRightYear()));

                tyreTable.addCell(new Phrase("Spare"));
                tyreTable.addCell(new Phrase(tyres.getSpareBrand()));
                tyreTable.addCell(new Phrase(tyres.getSpareTread() + "% / " + tyres.getSpareYear()));
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No tyre specifications saved yet."));
                cell.setColspan(3);
                tyreTable.addCell(cell);
            }
            document.add(tyreTable);

            // Emergency Checklist Info
            if (tyres != null) {
                Paragraph emergencyText = new Paragraph(String.format("Emergency Toolkit: Jack (%s), Handle (%s), Toolkit (%s), Triangle (%s), First Aid (%s)",
                        getBooleanText(tyres.getHasJack()), getBooleanText(tyres.getHasHandle()), getBooleanText(tyres.getHasToolkit()),
                        getBooleanText(tyres.getHasTriangle()), getBooleanText(tyres.getHasFirstAidBox())));
                emergencyText.setSpacingAfter(20);
                document.add(emergencyText);
            }

            // Interior & Electronics Section
            Paragraph intSec = new Paragraph("5. INTERIOR & ELECTRONICS", sectionFont);
            intSec.setSpacingAfter(10);
            document.add(intSec);

            PdfPTable intTable = new PdfPTable(2);
            intTable.setWidthPercentage(100);
            intTable.setSpacingAfter(20);

            if (interior != null) {
                addTableCell(intTable, "Battery Brand / Serial", interior.getBatteryBrand() + " / " + interior.getBatterySerialNumber());
                addTableCell(intTable, "AC Cooling", interior.getAcCooling());
                addTableCell(intTable, "Evaluator Valuation", interior.getEvaluatorValuation() != null ? "$" + interior.getEvaluatorValuation() : "N/A");
                addTableCell(intTable, "Central Locking System", interior.getCentralLock());
                addTableCell(intTable, "Sunroof", interior.getSunroof());
                addTableCell(intTable, "Remarks", interior.getRemarks());
            } else {
                PdfPCell cell = new PdfPCell(new Phrase("No interior checklist saved yet."));
                cell.setColspan(2);
                intTable.addCell(cell);
            }
            document.add(intTable);

            // General Remarks & Approval Info
            Paragraph remarksSec = new Paragraph("REMARKS & GENERAL INFOMATION", sectionFont);
            remarksSec.setSpacingAfter(10);
            document.add(remarksSec);

            PdfPTable remTable = new PdfPTable(2);
            remTable.setWidthPercentage(100);
            remTable.setSpacingAfter(20);

            addTableCell(remTable, "Inspection Status", inspection.getStatus().name());
            addTableCell(remTable, "Inspector", inspection.getInspector() != null ? inspection.getInspector().getFullName() : "N/A");
            addTableCell(remTable, "Rejection Reason", inspection.getRejectionReason() != null ? inspection.getRejectionReason() : "N/A");
            addTableCell(remTable, "General Remarks", remarks != null ? remarks.getInspectorRemarks() : "N/A");
            document.add(remTable);

            // Dynamic QR Code addition
            byte[] qrBytes = generateQrCodeImage("Inspection ID: " + inspection.getId() + 
                    "\nVehicle: " + (v != null ? v.getVehicleNumber() : "N/A") + 
                    "\nStatus: " + inspection.getStatus().name() + 
                    "\nDate: " + inspection.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            if (qrBytes != null) {
                Image qrImage = Image.getInstance(qrBytes);
                qrImage.setAlignment(Element.ALIGN_CENTER);
                qrImage.scaleToFit(120, 120);
                document.add(qrImage);
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private void addTableCell(PdfPTable table, String field, String value) {
        table.addCell(new Phrase(field));
        table.addCell(new Phrase(value != null ? value : "N/A"));
    }

    private String getBooleanText(Boolean value) {
        return Boolean.TRUE.equals(value) ? "YES" : "NO";
    }

    private byte[] generateQrCodeImage(String text) {
        try {
            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, 150, 150);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
