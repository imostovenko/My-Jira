(ns jira.server
  (:use [compojure.core :only [defroutes GET POST]])
  (:require
    [datomic.api :as d]
    [compojure.route    :as route]
    [jira.storage  :as storage]
    [org.httpkit.server :as httpkit]
    [ring.util.response :as response])
  (:gen-class))


(defn req->body [req]
  (-> req :body slurp))

(defroutes app
  (GET "/" []
    (response/resource-response "public/index.html"))

  (POST "/api/db/save" [:as req]
    (let [db (req->body req)]
      (storage/save-db db)
      (println "saved")
      {:status  200}))

  (GET "/api/db/get" [:as req]
    {:status  200
     :body (storage/get-db)})

  (route/resources "/" {:root "public"}))


(defn -main [& {:as args}]
  (let [port (or (get args "--port") "8080")]
    (println "Starting server at port" port)
    (httpkit/run-server #'app {:port (Long/parseLong port)})))



;;;; Datomic DB connection

(def url (str "datomic:free://localhost:4334/jira"))
(def conn (d/connect url))
(defn db [] (d/db conn))


;;;; -------- USERS ----------

(defn get-users-deep []
  (map #(d/pull (db) '[*] %)
    (d/q '[:find [?e ...]
           :in $
           :where [?e :user/login]]
      (db))))


(defn get-users-ids []
  (d/q '[:find [?e ...]
         :in $
         :where [?e :user/login]]
    (db)))


(defn add-user [login pass]
  (let [user [{:db/id #db/id[:db.part/user]
               :user/login login
               :user/pass pass
               :user/isAdmin false}]]
    (d/transact conn user))
  (get-users-deep))


(defn delete-user [u-id]
  (let [user [[:db.fn/retractEntity u-id]]]
    (d/transact conn user))
  (get-users-deep))


;;;;-------- PROJECTS --------

(defn get-projects-deep []
  (map #(d/pull (db) '[*] %)
    (d/q '[:find [?e ...]
           :in $
           :where [?e :project/title]]
      (db))))

(defn create-prj [title descr]
  (let [prj [{:db/id #db/id[:db.part/user]
              :project/title title
              :project/description descr}]]
    ;:project/author}]]
    (d/transact conn prj))
  (get-projects-deep))


(defn get-projects-ids []
  (d/q '[:find [?e ...]
         :in $
         :where [?e :project/title]]
    (db)))



