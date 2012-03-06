(ns recipe.routes
  (:use compojure.core
	[hiccup core page-helpers]
        recipe.manage-redis
	recipe.scraperecipe
	[recipe.wordlabels :only (boring-keywords)]
        [hiccup.middleware :only (wrap-base-url)] :reload)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(defn main-page [keyword-string]
  (let [keywords (clojure.string/split (clojure.string/lower-case keyword-string)  #"-")
        display-title  (clojure.string/join " " (map clojure.string/capitalize keywords))]

	(doseq [unsearched (filter #(= :search (check-keyword %)) keywords)]
	    (search-and-add-keyword unsearched))
	(let [good-keywords (filter #(and (= :good (check-keyword %)) (not (boring-keywords %))) keywords)
	      ingredients (construct-recipe good-keywords)]

    (html5
     [:head 
      [:title (str "Recipe-Roulette:" display-title)]
      (include-css "/css/bootstrap.css")
      (include-js "/scripts/jquery.js")
      (include-js "/scripts/main.js")]
     [:body
      [:div.container {:style "width: 700px; text-align: center;"}
       [:h1.page-title "Recipe Roulette"] 		
       [:div.well
	[:h1.recipe-title {:style "padding-bottom: 30px;"} display-title]
	(into [:ul {:style "text-align: left; padding-left: 180px;"} ] (map #(vector :li %) ingredients))]
	[:h3.lead "Make up a recipe name:"]
	[:p [:input#main-input {:class "span3" :type "text"} ]]
	[:p [:button#make-recipe-btn {:class "large btn primary"} "Create Recipe"]]]]
     ))))

(defn random-page []
  (main-page "Lemon")) ;(clojure.string/join " " (n-random-keywords 2))))

(defroutes main-routes
  (GET "/" [] (random-page))
  (GET "/:keywords" [keywords] (main-page keywords))
  (route/resources "/")
  (route/not-found "404rd!"))




(def app
  (-> (handler/site main-routes)
      (wrap-base-url)))