package com.sshtools.mobile.agent.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


public class SWTUtil {
	public static void center(Shell shell) {
		Rectangle d = shell.getDisplay().getPrimaryMonitor().getBounds();
		shell.setLocation(d.x + (d.width - shell.getSize().x) / 2, d.y + (d.height - shell.getSize().y) / 2);
	}

	public static Font newFont(Device device, Font font, int size, int style) {
		FontData[] fontData = font.getFontData();
		for (int i = 0; i < fontData.length; i++) {
			fontData[i].setHeight(size);
			fontData[i].setStyle(style);
		}
		return new Font(device, fontData);		
	}
	
	public static void showError(String title, String message) {

		safeAsyncExec(new Runnable() {
			public void run() {
				CustomDialog dialog = new CustomDialog(new Shell(), SWT.ICON_ERROR, SWT.ON_TOP | SWT.SYSTEM_MODAL, "Ok");
		        dialog.setText(title);
		        dialog.setMessage(message);
		        
		        dialog.open();
			}
		});
	}
	
	public static void showInformation(String title, String message, Runnable... callbacks) {

		safeAsyncExec(new Runnable() {
			public void run() {
				CustomDialog dialog = new CustomDialog(new Shell(), SWT.ICON_INFORMATION, SWT.ON_TOP | SWT.SYSTEM_MODAL, "Ok");
		        dialog.setText(title);
		        dialog.setMessage(message);
		        
		        dialog.open();
		        
		        for(Runnable callback : callbacks) {
		        	callback.run();
		        }
			}
		});
	}
	
	public static void showQuestion(String title, String message, Runnable onYes) {
		
		safeAsyncExec(new Runnable() {
			public void run() {
				CustomDialog dialog = new CustomDialog(new Shell(), SWT.ICON_QUESTION, SWT.ON_TOP | SWT.SYSTEM_MODAL, "Yes", "No");
		        dialog.setText(title);
		        dialog.setMessage(message);
		        
		        if(dialog.open() == "Yes") {
		        		onYes.run();
		        };
			}
		});
	}
	
	public static void safeAsyncExec(Runnable r) {
		if(Display.getDefault().getThread().equals(Thread.currentThread())) {
			r.run();
		} else {
			Display.getDefault().asyncExec(r);
		}
	}
}
