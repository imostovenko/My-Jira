;(ns jira.dat
;  (:require
;    [datomic.api :as d]))
;
;;(def url "datomic:free://localhost:4334/jira")
;;(def conn (d/connect url))
;;(defn db [] (d/db conn))
;
;
;(def uri "datomic:free://localhost:4334/jira")
;
;(def conn (d/connect uri))
;(def db (d/db conn))



;;;; -------- USERS ----------
;
;(defn get-users-deep []
;  (map #(d/pull (db) '[*] %)
;    (d/q '[:find [?e ...]
;           :in $
;           :where [?e :user/login]]
;      (db))))
;
;
;(defn get-users-ids []
;  (d/q '[:find [?e ...]
;         :in $
;         :where [?e :user/login]]
;    (db)))
;
;
;(defn add-user [login pass]
;  (let [user [{:db/id #db/id[:db.part/user]
;               :user/login login
;               :user/pass pass
;               :user/isAdmin false}]]
;    (d/transact conn user))
;  (get-users-deep))
;
;
;(defn delete-user [u-id]
;  (let [user [[:db.fn/retractEntity u-id]]]
;    (d/transact conn user))
;  (get-users-deep))
;
;
;;;;;-------- PROJECTS --------
;
;(defn get-projects-deep []
;  (map #(d/pull (db) '[*] %)
;    (d/q '[:find [?e ...]
;           :in $
;           :where [?e :project/title]]
;      (db))))
;
;(defn create-prj [title descr]
;  (let [prj [{:db/id #db/id[:db.part/user]
;              :project/title title
;              :project/description descr}]]
;    ;:project/author}]]
;    (d/transact conn prj))
;  (get-projects-deep))
;
;
;(defn get-projects-ids []
;  (d/q '[:find [?e ...]
;         :in $
;         :where [?e :project/title]]
;    (db)))
;
;
