(ns jira.admin.db
  (:require
    [jira.users.db :as udb]
    [jira.projects.db :as pdb]
    [clojure.string :as str]))


(defn set-admin!
  [login]
  (if (udb/u-exists? login)
    (do
      (swap! udb/users assoc-in [(udb/u-login->id login) :role] "admin")
      (for [i (keys @pdb/projects)]
        (pdb/add-u-to-p! i login)))
    {:error (str/join ["User with such login:" login "doesn't exist"])}))


(defn revoke-admin!
  [login]
  (if (udb/u-exists? login)
    (swap! udb/users assoc-in [(udb/u-login->id login) :role] "user")
    {:error (str/join ["User with such login:" login "doesn't exist"])}))
