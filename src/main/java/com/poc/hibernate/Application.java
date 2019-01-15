package com.poc.hibernate;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import com.poc.hibernate.model.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Application {
	private static SessionFactory factory = null;

	public static void main(String[] args) {

		System.out.print("go ? ");
		try(BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
			String input = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}


		System.out.println("============Set up Session Factory============");
		setUpSessionFactory();

		
		System.out.println("============Store Data============");
		storeData();
		

		/*System.out.println("============Update Data============");
		updateData();
*/
		shutdown();
	}

	public static void setUpSessionFactory() {
		// create sessionFactory
		try {
			factory = new Configuration().configure().buildSessionFactory();
		} catch (Throwable ex) {
			System.err.println("Failed to create sessionFactory object." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static void shutdown() {
		factory.close();
	}

	public static void storeData() {
		Session session = factory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			for (int i = 0; i < 20000; i++) {
				String text = Utilities.generatedRandomString();
				Data data = new Data(text);
				// "save() makes a new instance persistent"
				session.save(data);
				/* DEBUT PATCH
				if (i % 1000 == 0) {
					System.out.println("========================================== flushing, i = " + i);
					session.flush();
					session.clear();
				}*/
				/* FIN PATCH */
			}
			tx.commit();
		} catch (Exception e) {
			if (null != tx) {
				tx.rollback();
			}
		} finally {
			session.close();
		}
	}

	/*
	public static void updateData() {
		Session session = factory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			ScrollableResults dataCursor = session.createQuery("FROM Data").scroll();

			int count = 1;
			while (dataCursor.next()) {
				Data data = (Data) dataCursor.get(0);
				String newText = Utilities.generatedRandomString();
				data.setText(newText);
				session.update(data);

				if (count % 50 == 0) {
					System.out.println("==========================================count = " + count);
					session.flush();
					session.clear();
				}
				count++;
			}

			tx.commit();
		} catch (Exception e) {
			if (null != tx) {
				tx.rollback();
			}
		} finally {
			session.close();
		}
	}
	*/
}