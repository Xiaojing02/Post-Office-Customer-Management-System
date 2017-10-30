import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Created by FOX on 3/4/2017.
 */
public class PostOffice {

/*********Initialize all semaphores**********/
// initialize every semaphore and assign initial value to them.

    // The maximum capacity inside the post office is 10.
    private static Semaphore max_capacity = new Semaphore(10, true);
    // Postal worker waits to serve the customer until the customer is ready
    private static Semaphore cust_ready = new Semaphore(0, true);
    // Postal worker mailing a package waits until the current worker finishes using the scales.
    private static Semaphore scales = new Semaphore(1, true);
    // Customer waits until his request finished by the postal worker.
    private static Semaphore[] finished = new Semaphore[50];
    static {

        for(int i = 0; i < 50; i++) {

            finished[i] = new Semaphore(0, true);

        }

    }
    // Postal worker waits until customer finishes.
    private static Semaphore[] cust_finished = new Semaphore[50];
    static {

        for(int i = 0; i < 50; i++) {

            cust_finished[i] = new Semaphore(0, true);

        }

    }
    // Customer waits until a postal worker says serving them.
    private static Semaphore[] service = new Semaphore[50];
    static {

        for(int i = 0; i < 50; i++) {

            service[i] = new Semaphore(0, true);

        }

    }
    // Postal worker waits to take the order until the customer is ready.
    private static Semaphore[] task_ready = new Semaphore[50];
    static {

        for(int i = 0; i < 50; i++) {

            task_ready[i] = new Semaphore(0, true);

        }

    }
    // Only one person can access the queue at a time.
    private static Semaphore mutex1 = new Semaphore(1, true);
//    Initialize a queue used to store customer IDs by customers and retrieve those ID by postal workers.
    private static Queue<Integer> queue = new LinkedList<Integer>();

/******************************************/
//    Postal worker class
    private static class PostWorker implements Runnable {

        int workerID;    //ID of postal worker
        int getTask;     //the task get from the customer he's working for.
                         // The value is assigned by customer in customer class
                         // by using "postWorkers[getWorkerID].getTask = task;"
        static int count = 0;

        // Constructor of PostWorker
        //get a parameter workerID as his ID
        PostWorker(int workerID) {

            System.out.println("Postal worker " + workerID + " created.");
            this.workerID = workerID;

        }

        public void run() {

            int cust_num; //parameter used to store customer ID his working for.

            while (true && count!=maxsize) {

                // Postal worker waits until the customer is ready.
                cust_ready = semWait(cust_ready);

                // Postal worker waits to retrieve customer ID until the one currently editing the queue finishes.
                mutex1 = semWait(mutex1);

                cust_num = queue.poll(); // retrieve and delete a customer ID from the head of the queue
                count++;
                mutex1.release(); // signal that someone else can access the queue now

                customer[cust_num].getWorkerID = workerID;  // Tell his worker ID to the customer his working for
                service[cust_num].release(); // Tell the customer he's ready to serve.
                System.out.println("Postal worker " + workerID + " serving customer " + cust_num);

//              Postal worker waits until his customer tell him what to do.
                task_ready[cust_num] = semWait(task_ready[cust_num]); //Waiting for the customer to
                                                                      // assign value to getTask

                if(getTask == 0) {

                    // buy a stamps
                    serve(1000);

                } else if(getTask == 1) {

                    // mail a letter
                    serve(1500);

                } else if (getTask == 2){

                    // mail a package
                    //Only one postal worker can get the access to the scales
                    scales = semWait(scales);

                    System.out.println("Scales in use by postal worker " + workerID);
                    serve(2000);

                    System.out.println("Scales released by postal worker " + workerID);
                    scales.release();  // The scales is available by others

                }

                System.out.println("Postal worker " + workerID + " finished serving customer "
                        + cust_num);
                finished[cust_num].release(); // Tell the customer that he is good to go
                cust_finished[cust_num] = semWait(cust_finished[cust_num]);   // Wait the customer finishes

            }

        }

//***    Serve method for workers to finish customer's task.   *****//
//    get a parameter time as the time needs to finish the task, which is millisecond
        public void serve(long time) {

            // buy a stamps or mail a letter or mail a package
            try {

//                  sleep() is a static method
//                  time = 1000, then sleep 1 second
                Thread.sleep(time);

            } catch (Exception ex) {

                ex.printStackTrace();

            }
        }

    }


/******************************************/
//    Customer class
    private static class Customer implements Runnable {

        int customerID; // the ID of the customer
        int task;  // customer's task(buy a stamps or mail a letter or mail a package
        int getWorkerID; // the number get as the ID of the worker who is serving for him

     // Constructor of PostWorker
     // get a parameter customerID as his ID
        Customer(int customerID) {

            this.customerID = customerID;
            task = (int)(Math.random() * 3); //get a random task while initializing
            System.out.println("Customer " + customerID + " created.");

        }

        public void run() {

//            Customer waits to get into the office until there's room, and the capacity is 10.
            max_capacity = semWait(max_capacity);
            System.out.println( "Customer " + customerID + " enters post office" );
//            Only one person can access the queue at a time
            mutex1 = semWait(mutex1);
            queue.add(customerID);
//            Signal the postal worker that he is ready to be served.
            cust_ready.release();			
//            Signal other people can access the queue
            mutex1.release();

//            Customer waits until he is assigned a worker
            service[customerID] = semWait(service[customerID]);
            postWorkers[getWorkerID].getTask = task; // Tell the postal worker his task.
//          Print out the customer's task and who's working for him, then signal the worker to take it.
            if (task == 0) {

                System.out.println("Customer " + customerID + " asks postal worker " + getWorkerID +
                        " to buy stamps");

            } else if (task == 1) {

                System.out.println("Customer " + customerID + " asks postal worker " + getWorkerID +
                        " to mail a letter");

            } else if (task == 2) {

                System.out.println("Customer " + customerID + " asks postal worker " + getWorkerID +
                        " to mail a package");

            }
//          Customer signal the worker to take his task
            task_ready[customerID].release();

//          Customer waits until the worker finishes
            finished[customerID] = semWait(finished[customerID]);
//          Print out the customer finished his task
            if(task == 0) {

                System.out.println("Customer " + customerID + " finished buying stamps");

            } else if(task == 1) {

                System.out.println("Customer " + customerID + " finished mailing a letter");

            } else if(task == 2) {

                System.out.println("Customer " + customerID + " finished mailing a package");

            }

//            Customer signals the postal worker that he has finished
            cust_finished[customerID].release();
//            Print out the customer leaves
            System.out.println( "Customer " + customerID + " leaves post office" );
//            Signal that other customer can get into the post office
            max_capacity.release();
        }

    }


//**********    semWait method.         ***********//
//   get a Semaphore parameter wait then call acquire() method on it
    public static Semaphore semWait(Semaphore wait) {

        try {

            wait.acquire();

        } catch (Exception ex){

            ex.printStackTrace();

        }

        return wait;
    }


//*****************Create Threads in main() method.****************//
//   Construct and join Threads.
    final static int maxsize = 50; // There are 50 customers visiting post office
    static Customer customer[] = new Customer[maxsize]; //Create a new Customer object array
    static Thread myThread[] = new Thread[maxsize]; //Create a new thread array

    final static int workernum = 3; //There are 3 postal workers in the post office
    static PostWorker postWorkers[] = new PostWorker[workernum]; //Create a new postWorker object array
    static Thread wThread[] = new Thread[workernum]; //Create a new thread array


    public static void main(String[] args) {

        System.out.println("Simulating Post Office with " + maxsize + " customers and " + workernum + " postal workers");
        System.out.println();
        // create 3 postal worker threads and assign an ID to them
        for (int j = 0; j < workernum; j++){

            postWorkers[j] = new PostWorker(j); //Create postWorkers[j]
            wThread[j] = new Thread(postWorkers[j]); //Allocate a new thread object
            wThread[j].start(); // start the thread

        }

//        Create 50 customer threads and assign an ID to them
        for (int i = 0; i < maxsize; i++){

            customer[i] = new Customer(i);
            myThread[i] = new Thread(customer[i]);
            myThread[i].start();

        }

//        Join all customer threads and exit program.
        for(int i = 0; i < maxsize; i++) {

            try {

                myThread[i].join();
                System.out.println("Joined customer " + i);

            } catch (InterruptedException e) {

                e.printStackTrace();

            }
        }
//        Join all postal worker threads and exit program.
        for(int j = 0; j < workernum; j++) {

            try {

                wThread[j].join();
                System.out.println("Joined postal worker " + j);

            } catch (InterruptedException e) {

                e.printStackTrace();

            }
        }


    }


}
