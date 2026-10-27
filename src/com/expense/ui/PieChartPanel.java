package com.expense.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Biểu đồ tròn chi tiêu theo danh mục, vẽ bằng Java2D. */
public class PieChartPanel extends JPanel {
    private static final Color[] COLORS = {
            new Color(0x4E79A7), new Color(0xF28E2B), new Color(0xE15759), new Color(0x76B7B2),
            new Color(0x59A14F), new Color(0xEDC948), new Color(0xB07AA1), new Color(0xFF9DA7),
            new Color(0x9C755F), new Color(0xBAB0AC)
    };
    private Map<String, Double> data = new LinkedHashMap<>();
    private final NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    public PieChartPanel() {
        setPreferredSize(new Dimension(340, 220));
        setBorder(BorderFactory.createTitledBorder("Chi tiêu theo danh mục"));
    }

    public void setData(Map<String, Double> data) {
        this.data = data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Insets in = getInsets();
        int w = getWidth() - in.left - in.right, h = getHeight() - in.top - in.bottom;
        double total = data.values().stream().mapToDouble(Double::doubleValue).sum();

        if (total <= 0) {
            g.setColor(Color.GRAY);
            String s = "Chưa có dữ liệu chi tiêu";
            g.drawString(s, in.left + (w - g.getFontMetrics().stringWidth(s)) / 2, in.top + h / 2);
            g.dispose();
            return;
        }
        int d = Math.min(h - 10, w / 2);
        int x0 = in.left + 5, y0 = in.top + (h - d) / 2;
        double start = 90;
        int i = 0;
        for (Map.Entry<String, Double> e : data.entrySet()) {
            double ext = -360.0 * e.getValue() / total;
            g.setColor(COLORS[i % COLORS.length]);
            g.fill(new Arc2D.Double(x0, y0, d, d, start, ext, Arc2D.PIE));
            start += ext;
            i++;
        }
        // Chú thích
        int lx = x0 + d + 15, ly = in.top + 18;
        FontMetrics fm = g.getFontMetrics();
        i = 0;
        for (Map.Entry<String, Double> e : data.entrySet()) {
            if (ly > in.top + h - 5) break;
            g.setColor(COLORS[i % COLORS.length]);
            g.fillRect(lx, ly - 10, 10, 10);
            g.setColor(getForeground());
            String s = String.format("%s (%.0f%%)", e.getKey(), 100 * e.getValue() / total);
            g.drawString(s, lx + 16, ly);
            ly += fm.getHeight() + 2;
            i++;
        }
        g.dispose();
    }
}
