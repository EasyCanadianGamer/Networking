from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time
from bs4 import BeautifulSoup
import pandas as pd
from dotenv import load_dotenv
import os


load_dotenv()
# Replace these with your Amazon credentials
AMAZON_EMAIL = os.getenv('AMAZON_EMAIL')
AMAZON_PASSWORD = os.getenv('AMAZON_PASSWORD')

# Setup WebDriver
service = Service(executable_path='chromedriver.exe')
driver = webdriver.Chrome(service=service)
url = "https://www.amazon.com/"
driver.get(url)

# Log in to Amazon
try:
    # Click on "Sign in" button
    sign_in_button = WebDriverWait(driver, 10).until(
        EC.element_to_be_clickable((By.ID, "nav-link-accountList"))
    )
    sign_in_button.click()

    # Enter email
    email_field = WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.NAME, "email"))
    )
    email_field.send_keys(AMAZON_EMAIL)
    email_field.send_keys(Keys.RETURN)

    # Enter password
    password_field = WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.NAME, "password"))
    )
    password_field.send_keys(AMAZON_PASSWORD)
    password_field.send_keys(Keys.RETURN)

    # Wait for the user to manually enter the OTP
    print("Please enter the OTP manually on the Amazon login page.")
    WebDriverWait(driver, 120).until(
        EC.presence_of_element_located((By.ID, "nav-link-accountList"))  # Wait for login to complete
    )
    print("OTP entered successfully, and login complete.")
except Exception as e:
    print(f"Error during login: {e}")
    driver.quit()
    exit()

# Navigate to the product page
product_url = "https://www.amazon.com/Bose-QuietComfort-Cancelling-Headphones-Bluetooth/dp/B0CCZ26B5V/"
driver.get(product_url)

# Click on "See all reviews" button
try:
    see_all_reviews_button = WebDriverWait(driver, 10).until(
        EC.element_to_be_clickable((By.XPATH, "//a[contains(@data-hook, 'see-all-reviews-link-foot')]"))
    )
    see_all_reviews_button.click()  # Click the button
    time.sleep(5)  # Wait for the page to load
    print("Clicked 'See all reviews' button.")
except Exception as e:
    print(f"Error clicking 'See all reviews': {e}")

# Scrape reviews as before
data = []
while len(data) < 50:  # Stop after collecting 50 reviews
    page_source = driver.page_source
    soup = BeautifulSoup(page_source, 'html.parser')

    reviews = soup.find_all('span', {'data-hook': 'review-body'})
    ratings = soup.find_all('i', {'data-hook': 'review-star-rating'})

    for review, rating in zip(reviews, ratings):
        data.append({
            'rating': rating.text.strip() if rating else "N/A",
            'review': review.text.strip() if review else "N/A"
        })
        if len(data) >= 50:
            break

    # Click "Next" to go to the next page of reviews
    try:
        next_button = WebDriverWait(driver, 5).until(
            EC.element_to_be_clickable((By.XPATH, "//li[@class='a-last']/a"))
        )
        next_button.click()
        time.sleep(5)
    except:
        print("No more pages available.")
        break

# Find the Average Rating
average_rating = soup.find('span', {'data-hook': 'average-star-rating'})
average_rating_value = average_rating.text.strip() if average_rating else "N/A"
print(f"Average Rating: {average_rating_value}")


# Save data
df = pd.DataFrame(data)
df.to_csv('amazon_reviews.csv', index=False)
print("Final reviews saved to amazon_reviews.csv")

driver.quit()