package dev.vitorsilverio.gbcemu.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class OnScreenControlsView extends View {
    public interface ButtonListener {
        void onButtonState(int button, boolean pressed);
    }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dpad = new RectF();
    private final RectF dpadHorizontal = new RectF();
    private final RectF dpadVertical = new RectF();
    private final RectF buttonA = new RectF();
    private final RectF buttonB = new RectF();
    private final RectF select = new RectF();
    private final RectF start = new RectF();
    private ButtonListener listener = (button, pressed) -> {
    };

    public OnScreenControlsView(Context context) {
        super(context);
        setWillNotDraw(false);
        fill.setColor(0x6632383F);
        fill.setStyle(Paint.Style.FILL);
        stroke.setColor(0xCCF5F7FA);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3f);
        text.setColor(0xEEF5F7FA);
        text.setTextAlign(Paint.Align.CENTER);
        text.setFakeBoldText(true);
    }

    public void setButtonListener(ButtonListener listener) {
        this.listener = listener == null ? (button, pressed) -> {
        } : listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        layoutControls();
        drawDpad(canvas);
        drawRound(canvas, buttonA, 999f);
        drawRound(canvas, buttonB, 999f);
        drawRound(canvas, select, 999f);
        drawRound(canvas, start, 999f);
        drawDpadLabels(canvas);
        drawLabel(canvas, buttonA, "A", 0.46f);
        drawLabel(canvas, buttonB, "B", 0.46f);
        drawLabel(canvas, select, "SELECT", 0.28f);
        drawLabel(canvas, start, "START", 0.28f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        clearButtons();
        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            return true;
        }
        for (int i = 0; i < event.getPointerCount(); i++) {
            applyTouch(event.getX(i), event.getY(i));
        }
        return true;
    }

    private void layoutControls() {
        float width = getWidth();
        float height = getHeight();
        float margin = Math.min(width, height) * 0.08f;
        float button = Math.min(width, height) * 0.13f;
        boolean portrait = height >= width;
        float pad = Math.min(width, height) * (portrait ? 0.31f : 0.21f);
        if (portrait) {
            float bottom = height - margin;
            float systemBottom = bottom;
            float systemTop = systemBottom - button * 0.48f;
            float actionBottom = systemTop - button * 0.34f;
            float dpadBottom = actionBottom + button * 0.20f;
            dpad.set(margin, dpadBottom - pad, margin + pad, dpadBottom);
            buttonB.set(width - margin - button * 2.20f, actionBottom - button, width - margin - button * 1.20f, actionBottom);
            buttonA.set(width - margin - button, actionBottom - button * 1.65f, width - margin, actionBottom - button * 0.65f);
            select.set(width * 0.40f - button * 0.52f, systemTop, width * 0.40f + button * 0.52f, systemBottom);
            start.set(width * 0.60f - button * 0.52f, systemTop, width * 0.60f + button * 0.52f, systemBottom);
            return;
        }
        dpad.set(margin, height - margin - pad, margin + pad, height - margin);
        buttonA.set(width - margin - button, height - margin - pad * 0.70f, width - margin, height - margin - pad * 0.70f + button);
        buttonB.set(width - margin - button * 2.2f, height - margin - pad * 0.45f, width - margin - button * 1.2f, height - margin - pad * 0.45f + button);
        select.set(width * 0.42f - button * 0.55f, height - margin - button * 0.55f, width * 0.42f + button * 0.55f, height - margin);
        start.set(width * 0.58f - button * 0.55f, height - margin - button * 0.55f, width * 0.58f + button * 0.55f, height - margin);
    }

    private void drawDpad(Canvas canvas) {
        float arm = dpad.width() * 0.34f;
        dpadHorizontal.set(dpad.left, dpad.centerY() - arm * 0.5f, dpad.right, dpad.centerY() + arm * 0.5f);
        dpadVertical.set(dpad.centerX() - arm * 0.5f, dpad.top, dpad.centerX() + arm * 0.5f, dpad.bottom);
        float radius = arm * 0.28f;
        canvas.drawRoundRect(dpadHorizontal, radius, radius, fill);
        canvas.drawRoundRect(dpadVertical, radius, radius, fill);
        canvas.drawRoundRect(dpadHorizontal, radius, radius, stroke);
        canvas.drawRoundRect(dpadVertical, radius, radius, stroke);
    }

    private void drawRound(Canvas canvas, RectF rect, float radius) {
        canvas.drawRoundRect(rect, radius, radius, fill);
        canvas.drawRoundRect(rect, radius, radius, stroke);
    }

    private void drawDpadLabels(Canvas canvas) {
        text.setTextSize(dpad.width() * 0.18f);
        float centerX = dpad.centerX();
        float centerY = dpad.centerY();
        float offset = dpad.width() * 0.28f;
        drawTextCentered(canvas, "▲", centerX, centerY - offset);
        drawTextCentered(canvas, "▼", centerX, centerY + offset);
        drawTextCentered(canvas, "◀", centerX - offset, centerY);
        drawTextCentered(canvas, "▶", centerX + offset, centerY);
    }

    private void drawLabel(Canvas canvas, RectF rect, String label, float sizeRatio) {
        text.setTextSize(rect.height() * sizeRatio);
        drawTextCentered(canvas, label, rect.centerX(), rect.centerY());
    }

    private void drawTextCentered(Canvas canvas, String label, float centerX, float centerY) {
        Paint.FontMetrics metrics = text.getFontMetrics();
        float baseline = centerY - (metrics.ascent + metrics.descent) / 2.0f;
        canvas.drawText(label, centerX, baseline, text);
    }

    private void applyTouch(float x, float y) {
        if (dpad.contains(x, y)) {
            float cx = dpad.centerX();
            float cy = dpad.centerY();
            float dx = x - cx;
            float dy = y - cy;
            if (Math.abs(dx) > Math.abs(dy)) {
                listener.onButtonState(dx > 0 ? AndroidInputState.BUTTON_RIGHT : AndroidInputState.BUTTON_LEFT, true);
            } else {
                listener.onButtonState(dy > 0 ? AndroidInputState.BUTTON_DOWN : AndroidInputState.BUTTON_UP, true);
            }
        }
        if (buttonA.contains(x, y)) {
            listener.onButtonState(AndroidInputState.BUTTON_A, true);
        }
        if (buttonB.contains(x, y)) {
            listener.onButtonState(AndroidInputState.BUTTON_B, true);
        }
        if (select.contains(x, y)) {
            listener.onButtonState(AndroidInputState.BUTTON_SELECT, true);
        }
        if (start.contains(x, y)) {
            listener.onButtonState(AndroidInputState.BUTTON_START, true);
        }
    }

    private void clearButtons() {
        for (int button = 0; button < 8; button++) {
            listener.onButtonState(button, false);
        }
    }
}
