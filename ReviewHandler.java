package assignment6;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CS3354 Spring 2019 Review Handler Class assignment 2 solution
    @author metsis
    @author tesic
    @author wen
    @author Jeffrey Wilson
 */
public class ReviewHandler extends AbstractReviewHandler {

    private static int ID = 0;

    /**
     * Loads reviews from a given path. If the given path is a .txt file, then
     * a single review is loaded. Otherwise, if the path is a folder, all reviews
     * in it are loaded.
     * @param filePath The path to the file (or folder) containing the review(sentimentModel).
     * @param realClass The real class of the review (0 = Negative, 1 = Positive
     * 2 = Unknown).
     */
    
    // I didn't multi-thread this because it is impossible to keep the info of
    // counter across threads. The solution the prof presented in class was to
    // just use the temp list.
    @Override
    public void loadReviews(String filePath, int realClass) {
        File fileOrFolder = new File(filePath);
        try {
            if (fileOrFolder.isFile()) {
                // File
                if (filePath.endsWith(".txt")) {
                    // Import review
                    MovieReview review = readReview(filePath, realClass);
                    // Add to database
                    database.put(review.getId(), review);
                    //Output result: single file
                    SentimentAnalysisApp.outputArea.append("Review imported.\n");
                    SentimentAnalysisApp.outputArea.append("ID: " + review.getId() + "\n");
                    SentimentAnalysisApp.outputArea.append("Text: " + review.getText() + "\n");
                    SentimentAnalysisApp.outputArea.append("Real Class: " + review.getRealPolarity() + "\n");
                    SentimentAnalysisApp.outputArea.append("Classification result: " + review.getPredictedPolarity() + "\n");

                    SentimentAnalysisApp.log.info("Review imported. ID: " + review.getId() + "\n");
                    if (realClass == 2) {
                        SentimentAnalysisApp.outputArea.append("Real class unknown.\n");
                    } else if (realClass == review.getPredictedPolarity()) {
                        SentimentAnalysisApp.outputArea.append("Correctly classified.\n");
                    } else {
                        SentimentAnalysisApp.outputArea.append("Misclassified.\n");
                    }


                } else {
                    // Cannot import non-txt files
                    SentimentAnalysisApp.outputArea.append("Input file path is neither a txt file nor folder.\n");
                }
            } else {
                // Folder
                String[] files = fileOrFolder.list();
                String fileSeparatorChar = System.getProperty("file.separator");
                int counter = 0;
                for (String fileName : files) {
                    if (fileName.endsWith(".txt")) {
                        // Only import txt files
                        // Import review
                        MovieReview review = readReview(filePath + fileSeparatorChar + fileName, realClass);
                        // Add to database
                        database.put(review.getId(), review);
                        // Count correct classified reviews, only real class is known
                        if (realClass != 2 && review.getRealPolarity() == review.getPredictedPolarity()) {
                            counter++;
                        }
                    } else {
                        //Do nothing
                    }
                }
                // Output result: folder
                SentimentAnalysisApp.outputArea.append("Folder imported.\n");
                SentimentAnalysisApp.outputArea.append("Number of entries: " + files.length + "\n");
                SentimentAnalysisApp.log.info("Folder imported. Number of entries: " + files.length + "\n");
                // Only output accuracy if real class is known
                if (realClass != 2) {
                    SentimentAnalysisApp.outputArea.append("Correctly classified: " + counter + "\n");
                    SentimentAnalysisApp.outputArea.append("Misclassified: " + (files.length - counter) + "\n");
                    SentimentAnalysisApp.outputArea.append("Accuracy: " + ((double)counter / (double)files.length * 100) + "%\n");
                }
            }
        } catch (IOException e) {
            SentimentAnalysisApp.log.info(e.toString());
            e.printStackTrace();
        }

    }

    /**
     * Reads a single review file and returns it as a MovieReview object.
     * This method also calls the method classifyReview to predict the polarity
     * of the review.
     * @param reviewFilePath A path to a .txt file containing a review.
     * @param realClass The real class entered by the user.
     * @return a MovieReview object.
     * @throws IOException if specified file cannot be opened.
     */
    @Override
    public MovieReview readReview(String reviewFilePath, int realClass) throws IOException {
        // Read file for text
        Scanner inFile = new Scanner(new FileReader(reviewFilePath));
        String text = "";
        while (inFile.hasNextLine()) {
            text += inFile.nextLine();
        }
        // Remove the <br /> occurences in the text and replace them with a space
        text = text.replaceAll("<br />"," ");

        // Create review object, assigning ID and real class
        final MovieReview review = new MovieReview(ID, text, realClass);
        // Update ID
        ID++;
        // Classify review
        
        // Utilize a threadpool
        Thread thread = new Thread()	{
        	public void run(){
                classifyReview(review);
        }};
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        threadPool.execute(thread);
        return review;
    }

    /**
     * Deletes a review from the database, given its id.
     * @param id The id value of the review.
     */
    @Override
    public void deleteReview(int id) {

        if (!database.containsKey(id)) {
            // Review with given ID does not exist
            SentimentAnalysisApp.outputArea.append("ID " + id + " does not exist.\n");
            SentimentAnalysisApp.log.info("Review with ID " + id + "not found to be deleted.\n");
        } else {
            database.remove(id);
            SentimentAnalysisApp.outputArea.append("Review with ID " + id + " deleted.\n");
            SentimentAnalysisApp.log.info("Review with ID " + id + " deleted.\n");
        }
    }

    /**
     * Loads review database.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void loadSerialDB() {
        SentimentAnalysisApp.outputArea.append("Reading database...\n");
        // serialize the database
        InputStream file = null;
        InputStream buffer = null;
        ObjectInput input = null;
        try {
            file = new FileInputStream(DATA_FILE_NAME);
            buffer = new BufferedInputStream(file);
            input = new ObjectInputStream(buffer);

            database = (Map<Integer, MovieReview>)input.readObject();
            SentimentAnalysisApp.outputArea.append(database.size() + " entry(s) loaded.\n");

            SentimentAnalysisApp.log.info("Database loaded. " + database.size() + " entry(s) loaded.\n");
            // Find the current maximum ID
            if (! database.isEmpty()) {
                int currMaxId = Collections.max(database.keySet());
                ID = currMaxId + 1;
            } else {
                ID = 1;
            }

            input.close();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            SentimentAnalysisApp.log.info(e.toString());
            e.printStackTrace();
        } finally {
            close(file);
        }
        SentimentAnalysisApp.outputArea.append("Done.\n");
    }

    /**
     * Searches the review database by id.
     * @param id The id to search for.
     * @return The review that matches the given id or null if the id does not
     * exist in the database.
     */
    @Override
    public MovieReview searchById(int id) {
        if (database.containsKey(id)) {
            return database.get(id);
        }
        return null;
    }

    /**
     * Searches the review database for reviews matching a given substring.
     * @param substring The substring to search for.
     * @return A list of review objects matching the search criterion.
     */
    @Override
    public List<MovieReview> searchBySubstring(String substring) {
        List<MovieReview> tempList = new ArrayList<MovieReview>();

        for (Map.Entry<Integer, MovieReview> entry : database.entrySet()){
            if (entry.getValue().getText().contains(substring)) {
                tempList.add(entry.getValue());
            }
        }

        if (!tempList.isEmpty()) {
            return tempList;
        } else {
            // No review has given substring
            return null;
        }

    }
}