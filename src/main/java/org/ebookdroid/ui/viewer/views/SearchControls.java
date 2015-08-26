package org.ebookdroid.ui.viewer.views;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.ebookdroid.R;
import org.ebookdroid.ui.viewer.ViewerActivity;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.actions.params.EditableValue;

public class SearchControls extends LinearLayout {

	private final EditText m_edit;
	private final LinearLayout m_locationLayout;
	private final TextView m_total;
	private final TextView m_current;
	private boolean m_showTotals = false;

	public SearchControls(final ViewerActivity parent) {
		super(parent);
		setVisibility(View.GONE);
		setOrientation(LinearLayout.VERTICAL);

		LayoutInflater.from(parent).inflate(R.layout.seach_controls, this, true);
		ImageButton prevButton = (ImageButton) findViewById(R.id.search_controls_prev);
		ImageButton nextButton = (ImageButton) findViewById(R.id.search_controls_next);
		m_edit = (EditText) findViewById(R.id.search_controls_edit);
		m_locationLayout = (LinearLayout) findViewById(R.id.search_controls_location);
		m_locationLayout.setVisibility(View.GONE);
		m_total = (TextView) findViewById(R.id.search_controls_total);
		m_current = (TextView) findViewById(R.id.search_controls_current);

		ActionEx forwardSearch = parent.getController().getOrCreateAction(R.id.actions_doSearch);
		ActionEx backwardSearch = parent.getController().getOrCreateAction(R.id.actions_doSearchBack);

		forwardSearch.addParameter(new EditableValue("input", m_edit)).addParameter(new Constant("forward", "true"));
		backwardSearch.addParameter(new EditableValue("input", m_edit)).addParameter(new Constant("forward", "false"));

		prevButton.setOnClickListener(backwardSearch);
		nextButton.setOnClickListener(forwardSearch);
		m_edit.setOnEditorActionListener(forwardSearch);
	}

	public void setVisibility(int visibility) {
		super.setVisibility(visibility);
		if (visibility == VISIBLE) {
			m_edit.requestFocus();
		}
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		return false;
	}

	public int getActualHeight() {
		return m_edit.getHeight();
	}

	public void setTerm(String searchTerm) {
		m_edit.setText(searchTerm);
		setVisibility(VISIBLE);
	}

	public void setTotal(int total) {
		m_total.setText(""+total);
		if (total > 0 && m_showTotals) {
			m_locationLayout.setVisibility(View.VISIBLE);
		} else {
			m_locationLayout.setVisibility(View.GONE);
		}
	}

	public void setCurrent(int current) {
		m_current.setText(""+current);
	}

	public void setShowTotals(boolean showTotals) {
		this.m_showTotals = showTotals;
	}
}
