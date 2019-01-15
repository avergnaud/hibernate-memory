# Hibernate Batch memory analysis

## setup

https://grokonez.com/hibernate/resolve-hibernate-outofmemoryerror-problem-hibernate-batch-processing

CREATE DATABASE testdb CHARACTER SET utf8 COLLATE utf8_general_ci ;

use testdb;

CREATE TABLE data(
   id INT NOT NULL AUTO_INCREMENT,
   text VARCHAR(20000) NOT NULL,
   PRIMARY KEY (id)
);

après run :

SELECT table_schema "DB Name",
        ROUND(SUM(data_length + index_length) / 1024 / 1024, 1) "DB Size in MB"
FROM information_schema.tables
GROUP BY table_schema;

+--------------------+---------------+

| DB Name            | DB Size in MB | 

+--------------------+---------------+

| testdb             |         582.5 |

DROP DATABASE testdb;

# Hibernate batch - one session, no jdbc batching, no flush

## symptôme

OutOfMemoryError avec Xmx128m :

| testdb             |          82.5 |

[OutOfMemoryError exemple 1](files/OutOfMemoryError_1.txt)

ou

[OutOfMemoryError exemple 2](files/OutOfMemoryError_2.txt)

## mesures

### Heap histogram ?

#### jcmd

jcmd << pid >> GC.class_histogram

[jcmd histograms](files/jcmd_histograms.txt)

Le histogram ne remonte que le flat ou shallow size des objets, et pas le retained size

#### jmap

jmap -histo:live << pid >>

[jmap histograms](files/jmap_histograms.txt)

Même conclusion que avec jcmd


### HeapDumpOnOutOfMemoryError ?

-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/ubuntu/dev4/heapdumps

jhat, eclipse MAT, jvisualvm (?)

#### jhat

jhat /home/ubuntu/dev4/heapdumps/java_pid6985.hprof

http://localhost:7000/

compliqué : https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/tooldescr012.html#BABJFDGC

#### jvisualvm ?

...

#### eclipse MAT

![alt text](https://raw.githubusercontent.com/avergnaud/hibernate-memory/master/images/eclipse_mat_1.png "eclipse MAT 1")

![alt text](https://raw.githubusercontent.com/avergnaud/hibernate-memory/master/images/eclipse_mat_2.png "eclipse MAT 2")

### heap dumps au cours de l'exéc ?

pour voir l'évolution. on verrait grossir le _retained size_ du org.hibernate.engine.internal.StatefulPersistenceContext

### logs GC ?

...

## analyse

https://docs.jboss.org/hibernate/core/3.3/api/org/hibernate/engine/StatefulPersistenceContext.html

https://docs.jboss.org/hibernate/core/3.3/api/org/hibernate/engine/PersistenceContext.html

"Holds the state of the persistence context, including the first-level cache, entries, snapshots, proxies, etc"

Stack du save() :

[Stack du save](files/save_stack.txt)

ligne 239 org.hibernate.event.internal.AbstractSaveEventListener.performSaveOrReplicate
```
EntityEntry original = source.getPersistenceContext().addEntry(
```
source.getPersistenceContext() est le StatefulPersistenceContext remonté par eclipse mat
A chaque save(), on ajoute l'entité dans le StatefulPersistenceContext. Comme expliqué ici https://vladmihalcea.com/a-beginners-guide-to-jpahibernate-flush-strategies/ "Hibernate tries to defer the Persistence Context flushing up until the last possible moment. This strategy has been traditionally known as transactional write-behind. The write-behind is more related to Hibernate flushing rather than any logical or physical transaction. During a transaction, the flush may occur multiple times.
The flushed changes are visible only for the current database transaction. Until the current transaction is committed, no change is visible by other concurrent transactions. The persistence context, also known as the first level cache, acts as a buffer between the current entity state transitions and the database. In caching theory, the write-behind synchronization requires that all changes happen against the cache, whose responsibility is to eventually synchronize with the backing store."

# Hibernate batch - one session, no jdbc batching, with flush

Application.java
```
for (int i = 0; i < 20000; i++) {
	String text = Utilities.generatedRandomString();
	Data data = new Data(text);
	// "save() makes a new instance persistent"
	session.save(data);
        
	if (i % 1000 == 0) {
		System.out.println("flushing, i = " + i);
		session.flush();
		session.clear();
	}
}

```

## analyse

(java 8)

-XX:+PrintGCDetails -XX:-PrintGCTimeStamps -XX:+PrintGCDateStamps -Xloggc:/home/ubuntu/dev4/gclogs/logs.txt -XX:-UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=10m -XX:+PrintReferenceGC -XX:-PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime

-Xmx128m -XX:+PrintGCDetails -XX:-PrintGCTimeStamps -XX:+PrintGCDateStamps -Xloggc:/home/ubuntu/dev4/gclogs/logs.txt -XX:-UseGCLogFileRotation -XX:NumberOfGClogFiles=5 -XX:GCLogFileSize=10m -XX:+PrintReferenceGC -XX:-PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime

-server -Xmx128m -XX:+PrintGCDetails -XX:-PrintGCTimeStamps -XX:+PrintGCDateStamps -Xloggc:/home/ubuntu/dev4/gclogs/logs.txt -XX:-UseGCLogFileRotation -XX:GCLogFileSize=10m -XX:+PrintReferenceGC -XX:-PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime

GCViewer (https://github.com/chewiebug/GCViewer)
gceasy.io (http://gceasy.io/ https://tier1app.files.wordpress.com/2014/12/outofmemoryerror2.pdf)

http://fasterj.com/tools/gcloganalysers.shtml

### gceasy

[OutOfMemoryError pas de flush](files/GCeasy-report-logs_no-flush-OOMError.pdf)

gceaysy ne détecte pas de memory leak, contrairement à eclipse MAT

[flush tous les 50 save](files/GCeasy-report-logs_flush-50.pdf)

[flush tous les 1000 save](files/OutOfMemoryError_1.txt)

Il faudrait comparer le Throughput entre les stratégies %50 et %1000

### GCViewer

https://github.com/chewiebug/GCViewer/wiki/Changelog

java -jar /home/ubuntu/dev4/gcviewer-1.35.jar

bof

 