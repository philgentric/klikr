// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.ui.progress;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klikr.look.my_i18n.My_I18n;
import klikr.util.Check_remaining_RAM;
import klikr.util.Kontext;
import klikr.util.execute.actor.Actor_engine;
import klikr.look.Look_and_feel_manager;
import klikr.util.ui.Jfx_batch_injector;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Progress_window implements Hourglass
//**********************************************************
{
    public final Kontext context;
	private final int timeout_s;
	Stage stage;
	ImageView iv;
	long start;
	public final CountDownLatch latch = new CountDownLatch(1);
	Label in_flight_label;
	Label ETA_label;
    Progress_spinner spinner;

	//**********************************************************
	public static Optional<Hourglass> show(
            String wait_message,
            int timeout_s,
            Kontext context) // if the context.aborter() is not null there will be an abort button in the popup/spinner
	//**********************************************************
	{
		if (Check_remaining_RAM.low_memory.get())
			return Optional.empty();
		Progress_window local = new Progress_window(timeout_s, context);
		launch(local, wait_message,context);
		return Optional.of(local);
	}


	//**********************************************************
	public static Optional<Hourglass> show_with_in_flight(
            AtomicInteger in_flight,
            String wait_message,
            int timeout_s,
            Kontext context)
	//**********************************************************
	{
		if (Check_remaining_RAM.low_memory.get()) return Optional.empty();
		Progress_window local = new Progress_window( timeout_s, context);
		launch(local, wait_message,context);
		local.report_progress_and_close_when_finished(in_flight);
		return Optional.of(local);
	}
/*
	//**********************************************************
	public static Optional<Hourglass> show_with_in_flight_and_aborter(
			AtomicInteger in_flight,
			String wait_message,
			int timeout_s,
			Kontext context)
	//**********************************************************
	{
		if (Check_remaining_RAM.low_memory.get()) return Optional.empty();
		Progress_window local = new Progress_window( timeout_s, context);
		launch(local, wait_message,context);
		local.report_progress_and_close_when_finished(in_flight);
		return Optional.of(local);
	}
*/

	//**********************************************************
	private static Hourglass launch(
            Progress_window local,
            String wait_message,
            Kontext context)
	//**********************************************************
	{
		if ( Platform.isFxApplicationThread())
		{
			local.define_fx(wait_message,context);
		}
		else
		{
			Jfx_batch_injector.inject(()->local.define_fx(wait_message,context),context);
		}
		return local;
	}

	//**********************************************************
	private Progress_window(int timeout_s_, Kontext context)
	//**********************************************************
	{
		this.context = context;
        timeout_s = timeout_s_;
	}


	//**********************************************************
	private void define_fx(String wait_message, Kontext context)
	//**********************************************************
	{
		start = System.currentTimeMillis();
		context.log("Progress_window: "+wait_message);
		stage = new Stage();
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        stage.setMinWidth(300);
		stage.setX(context.owner().getX()+100);
        stage.setY(context.owner().getY()+100);

        VBox vbox = new VBox();
		Look_and_feel_manager.set_region_look(vbox,context.logger());

		vbox.setAlignment(javafx.geometry.Pos.CENTER);

        switch(Look_and_feel_manager.get_instance(context.logger()).get_look_and_feel_style())
        {
            case light, dark, wood:
				Image film = Look_and_feel_manager.get_running_film_icon(context.logger());
				if( film != null) {
					iv = new ImageView(film);
					iv.setFitHeight(100);
					iv.setPreserveRatio(true);
					vbox.getChildren().add(iv);
				}
                break;
            case modena:
            case materiol:
            default:
                spinner = new Progress_spinner();
                Pane pane = spinner.start();
                vbox.getChildren().add(pane);
                break;
        }

		{
			in_flight_label = new Label();
			vbox.getChildren().add(in_flight_label);
			Look_and_feel_manager.set_label_look(in_flight_label,context.logger());
		}
		{
			ETA_label = new Label();
			vbox.getChildren().add(ETA_label);
			Look_and_feel_manager.set_label_look(ETA_label,context.logger());
		}
        if ( context.aborter()!=null)
		{
			Button abort = new Button(My_I18n.get_I18n_string("Abort",context));
            Look_and_feel_manager.set_region_look(abort,true,context.logger());
			//abort.setBorder(Look_and_feel_manager.get_border(context.logger()));
			vbox.getChildren().add(abort);
			abort.setOnAction(e -> {
				context.log("Progress_window abort BUTTON !");
				context.abort("aborted by progress window button");
			});
		}


		Scene scene = new Scene(vbox);

		//stage.setTitle(wait_message);
        in_flight_label.setText(wait_message);
		stage.setScene(scene);
		stage.show();

		stage.addEventHandler(KeyEvent.KEY_PRESSED,
				key_event -> {
					if (key_event.getCode() == KeyCode.ESCAPE) {
						stage.close();
						key_event.consume();
					}
				});

		Runnable monitor = () -> {
			try {
                int count = 0;
                for(;;)
				{
					boolean b = latch.await(1, TimeUnit.SECONDS);
					if (!b)
					{
						// timeout
						if ( context.aborter() != null)
						{
							if (context.should_abort())
							{
								has_ended("aborted",false);
								return;
							}
						}
                        count++;
                        if ( count > timeout_s)
						{
							has_ended("Time count out", false);
							return;
						}
						continue;
                    }
                    has_ended(wait_message + "... finished!", true);
                    return;
                }
			} catch (InterruptedException e) {
				context.log("Show running man wait interrupted");
			}
		};
		Actor_engine.execute(monitor,"Progress window monitor", context.logger());
	}
	
	//**********************************************************
	public void has_ended(String message, boolean sleep)
	//**********************************************************
	{
		//logger.log("running man has ended "+error_message);

		long sleep_time = System.currentTimeMillis()-start;
		if ( sleep_time > 3000) sleep_time = 3000;
		Jfx_batch_injector.inject(() -> {
			//stage.setTitle(message);
            in_flight_label.setText(message);
            if (iv != null)
            {
				Image end = Look_and_feel_manager.get_the_end_icon(context.logger());
                if( end != null) iv.setImage(end);
            }
		},context);

		if ( sleep) {
			long finalSleep_time = sleep_time;
			Runnable r = () -> {
				try {
					Thread.sleep(finalSleep_time);
				} catch (InterruptedException e) {
				}
				Jfx_batch_injector.inject(() -> stage.close(),context);

			};

			Actor_engine.execute(r, "sleep and close",context.logger());
		}
		else
		{
			Jfx_batch_injector.inject(() -> stage.close(),context);
		}
	}

	@Override // Hourglass
	//**********************************************************
	public void close() {
		latch.countDown();
	}
	//**********************************************************



	//**********************************************************
	private void report_progress_and_close_when_finished(AtomicInteger in_flight)
	//**********************************************************
	{

		Runnable tracker = () -> {
			long start = System.currentTimeMillis();
			double start_amount = in_flight.doubleValue();
            for(;;)
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
				double in_flight_local = in_flight.doubleValue();

				if( in_flight_local<= 0)
				{
					close();
					return;
				}

				// in case in_flight increases in the meantime...
				if ( in_flight_local > start_amount)
				{
					start = System.currentTimeMillis();
					start_amount = in_flight_local;
				}

				long elapsed = System.currentTimeMillis() - start;
				double done = start_amount - in_flight_local;
				double speed = done / elapsed * 1000; // items/s
				long eta_s = (long)(in_flight_local / speed);
				long eta_m = 0L;
				long eta_h = 0L;
				long eta_day = 0L;
				if ( eta_s > 60L)
				{
					eta_m = eta_s / 60L;
					eta_s = eta_s % 60L;
					if ( eta_m > 60L)
					{
						eta_h = eta_m / 60L;
						eta_m = eta_m % 60L;
						if (eta_h > 60L)
						{
							eta_day = eta_h / 60L;
							eta_h = eta_h % 60L;
						}
					}
				}
				String eta_string;
				if ( eta_day > 0L) eta_string = String.format("ETA: %02d days %02d hours", eta_day, eta_h);
				else if ( eta_h > 0L) eta_string = String.format("ETA: %02d hours %02d minutes", eta_h, eta_m);
				else if ( eta_m > 0) eta_string = String.format("ETA: %02d minutes %02d seconds", eta_m, eta_s);
				else eta_string = String.format("ETA: %02d seconds", eta_s);

				String finalEta_string = eta_string;
				Jfx_batch_injector.inject(()->
				{
					ETA_label.setText(finalEta_string);
					in_flight_label.setText("Items in flight: " +in_flight_local);
				},context);

            }
        };
		Actor_engine.execute(tracker, "Progress window ETA monitor",context.logger());
	}

	//**********************************************************
    public void set_text(String text)
	//**********************************************************
	{
		if (stage != null) {
			Jfx_batch_injector.inject(() -> in_flight_label.setText(text), context);
		}
	}
}
