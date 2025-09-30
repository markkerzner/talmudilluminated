# Talmud Illuminated scraper

## About the project

* The code here scrapes the content of the blog [Talmud Illuminated](http://mkerzner.blogspot.com/)
* The goal of the [Talmud Illuminated Project](https://talmudilluminated.com/) is to bring the [benefits](doc/benefits.md) of
study to everyone who wants it. These [benefits](doc/benefits.md) are multiple and more can be added.
* This scraping project was started on Tisha B'Av 2021. Tisha B'Av is the saddest day of Jewish history. 
  It is a fast day when Torah and Talmud study are prohibited. However, this project was perfect for Tisha b'Av.
In scraping, one is not learning Torah, just creating a structure for the data. So, it may be a mitzvah, but a mitzvah is not prohibited on Tisha B'Av
At the same time, it is not business (which is also not encouraged on this day).
  
## About scraping

* The code iterates through every Talmud volume (masechet) name, and through every page, by the number of pages in the masechet.
* Then, it searches for this page using Google Blogger API, get a response in JSON, and parses through the response.
The search may bring back a few pages, and the parser find the one it is looking for using the title.
* For example, the code may be looking for "bava kamma 48" string. For each JSON result, 
it finds the answer where the _title_ field is "bava kamma 48".
* All pages are stored into this project under the _content_ folder, and committed to GitHub.  

## About the dates

* The scraping project was basically completed in six days, from Tisha B'Av till Tu B'Av, 2021. That year, Tu B'Av
fell out on Shabbat. The Talmud Illuminated project was started on the previous Tu B'Av that fell out on Shabbat, 2008.
[Here is the start](http://mkerzner.blogspot.com/2008/08/purpose-of-daf-in-100-words.html).
This part of the project thus took exactly 13 years on the Jewish calendar.
  
## Instructions

1. Crawl (run Full Crawl configuration)
2. QA (run QA configuration)
3. MakeSite (run MakeSite configuration)
4. Copy the site to TalmudIlluminatedContent repo
   * `cp -r site/* ../TalmudIlluminatedContent/`
5. Deploy from there (open TalmudIlluminatedContent project and run deploy.sh)

* Custom GPT:
* [](https://chatgpt.com/g/g-68d6ee5a5c008191a1919c20e0777157-moses-ai)
