import time
import random
import csv
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.service import Service
from bs4 import BeautifulSoup
from textblob import TextBlob

# Function for random delay
def random_delay():
    time.sleep(random.randint(2, 6))

# Initialize WebDriver with Service
def setup_driver():
    service = Service(executable_path='chromedriver.exe')  # Update with the correct path to your WebDriver
    options = webdriver.ChromeOptions()
    options.add_argument("--start-maximized")  # Optional: Start the browser maximized
    return webdriver.Chrome(service=service, options=options)

# Scrape reviews from Best Buy
def scrape_reviews(url):
    driver = setup_driver()
    driver.get(url)
    random_delay()

    # Click the "Reviews" tab if necessary
    # try:
    #     reviews_tab = driver.find_element(By.XPATH, "//button[contains(text(), 'Reviews')]")
    #     reviews_tab.click()
    #     random_delay()
    # except Exception as e:
    #     print(f"Error navigating to reviews: {e}")

# Load all reviews using "See All Customer Reviews" button
    while True:
        try:
            see_all_customers_button = driver.find_element(By.XPATH, "//a[contains(@class, 'see-more-reviews') and @role='button']")
            driver.get(see_all_customers_button.get_attribute("href"))  # Navigate to the link
            random_delay()
            break  # Exit the loop after navigating to the reviews page
        except Exception:
            print("See All Customer Reviews button not found or no more reviews to load.")
            break


    # Get page source and parse with BeautifulSoup
    page_source = driver.page_source
    driver.quit()

    soup = BeautifulSoup(page_source, "lxml")
    review_elements = soup.find_all("p", {"class": "pre-white-space"})  # Adjust class if needed
    reviews = [review.text.strip() for review in review_elements]
    return reviews

# Analyze reviews for sentiment
def analyze_reviews(reviews):
    sentiment_scores = []
    for review in reviews:
        analysis = TextBlob(review)
        sentiment_scores.append(analysis.sentiment.polarity)

    # Calculate adjusted average sentiment score
    adjusted_score = sum(sentiment_scores) / len(sentiment_scores) if reviews else 0
    return sentiment_scores, adjusted_score

# Save reviews and sentiment scores to a CSV file
def save_to_csv(reviews, sentiment_scores, adjusted_score, filename="bestbuy_reviews.csv"):
    with open(filename, "w", newline="", encoding="utf-8") as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(["Review", "Sentiment Score"])  # Header row
        for review, score in zip(reviews, sentiment_scores):
            writer.writerow([review, score])
        writer.writerow([])  # Blank line
        writer.writerow(["Adjusted Average Sentiment Score", round(adjusted_score, 2)])
    print(f"Saved reviews and scores to {filename}.")

# Main function
def main():
    url = "https://www.bestbuy.com/site/bose-quietcomfort-ultra-wireless-noise-cancelling-over-the-ear-headphones-lunar-blue/6589916.p?skuId=6589916"
    print("Scraping reviews...")
    reviews = scrape_reviews(url)

    if not reviews:
        print("No reviews found. Exiting.")
        return

    print(f"Scraped {len(reviews)} reviews.")
    print("Analyzing reviews...")
    sentiment_scores, adjusted_score = analyze_reviews(reviews)
    print(f"Adjusted Average Sentiment Score: {adjusted_score:.2f}")
    

    # Save to CSV
    save_to_csv(reviews, sentiment_scores, adjusted_score)

if __name__ == "__main__":
    main()
