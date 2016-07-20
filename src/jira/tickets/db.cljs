(ns jira.tickets.db
  (:require
    [jira.users.db :as udb]
    [jira.projects.db :as pdb]

    [ajax.core :refer [GET POST]]
    [clojure.string :as str]
    [jira.util :as u :refer [any?]]))


(enable-console-print!)

;;;; ------------ INTIT ----------

(defonce tickets (atom {}))

(defonce latest-t-id (atom 0))

(defn next-t-id [] (swap! latest-t-id inc))

(def t-type {:0 "TODO" :1 "FIXME"})

(def t-prior {:0 "High" :1 "Medium" :2 "Low"})

(def t-status {:0 "Open" :1 "In Progress" :2 "Done"})

(defn empty-t
  [next-t-id p-id type prior status subj descr assignee]
  {:id        next-t-id
   :project     p-id
   :type        type
   :prior       prior
   :status      status
   :subj        subj
   :description descr
   :creator     (udb/u-login->id @udb/current-u)
   :assignee    assignee})


;;;; ---------------- GET -----------

(defn t-exists?
  [t-id]
  (contains? @tickets t-id))


(defn get-t
  [t-id]
  (@tickets t-id))


(defn get-tt-of-p
  "All tickets ids of project"
  [p-id]
  ((pdb/get-p p-id) :tickets))



;;;; ----------------CREATE ----------

(declare add-t-to-p!)
(defn create-t!
  ([p-id subj descr]
   (create-t! p-id :0 :0 :0 subj descr (udb/u-login->id @udb/current-u)))

  ([p-id type prior status subj descr assignee]
   (if (and (pdb/p-exists? p-id)
         (udb/u-exists? (udb/u-id->login assignee))
         (contains? t-type type)
         (contains? t-prior prior)
         (contains? t-status status))
     (let [t-id (next-t-id)]
       (cond
         (empty? subj)
         {:error "Ticket subject should not be empty. Please specify!"}
         :else
         (do
           (swap! tickets assoc t-id (empty-t t-id p-id type prior status subj descr assignee))
           (add-t-to-p! p-id t-id)
           t-id)))
     {:error (str/join ["project or users (" p-id "or" assignee ") doesn't exist! Check is t-type (:0 :1)
     and t-prior (:0 :1 :2) are valid"])})))



;;;;--------- FILTER --------------

(defn filter-by-* [tickets key values]
  (if (empty? values)
    tickets
    (u/filter-vals #(contains? (set values) (get % key)) tickets)))


(defn filter-by-users [users tickets]
  (filter-by-* tickets :assignee users))


(defn filter-by-statuses [statuses tickets]
  (filter-by-* tickets :status statuses))


(defn filter-by-types [types tickets]
  (filter-by-* tickets :type types))


(defn filter-by-priors [priors tickets]
  (filter-by-* tickets :prior priors))


(defn search [s-string tickets]
  (if (empty? s-string)
    tickets
    (u/filter-vals #(str/includes? (str/lower-case (get % :subj)) (str/lower-case s-string)) tickets)))


(defn super-filter [settings tickets]
  (->> tickets
    (filter-by-users (:users settings))
    (filter-by-statuses (:statuses settings))
    (filter-by-types (:types settings))
    (filter-by-priors (:priors settings))))



(defn filter-by-project
  ([]
   (filter-by-project @tickets @pdb/selected-p))
  ([tickets project]
   (->> (get-tt-of-p project)
    (select-keys tickets))))


;;;; ---------------- COUNT -------------

(defn count-tickets []
  (count @tickets))


(defn count-tickets-by-*
  ([key vals]
   (count-tickets-by-* @tickets key vals))
  ([tickets key vals]
   (-> (filter-by-* tickets key vals) count)))


;;;; --------------- UPDATE -------------

(defn update-t!
  [t-id k v]
  (if (t-exists? t-id)
    (swap! tickets update t-id merge {k v})
    {:error (str/join ["Ticket with ID:" t-id "doesn't exist, nothing to update!"])}))


(defn update-t-subj!
  [t-id subj]
  (update-t! t-id :subj subj))


(defn update-t-descr!
  [t-id descr]
  (update-t! t-id :description descr))


(defn reassign-t!
  [t-id assi]
  (if (udb/u-exists? assi)
    (do (pdb/add-u-to-p! (:project (get-t t-id)) assi)
        (update-t! t-id :assignee (udb/u-login->id assi)))

    {:error (str/join ["User with login:" assi "doesn't exist, so can't reassign!"])}))


(defn update-t-type!
  [t-id type]
  (if (contains? #{:0 :1} type)
    (update-t! t-id :type type)
    {:error (str/join ["Status" type "doesn't exist, use the following only: open, in progress, done."])}))


(defn update-t-status!
  [t-id status]
  (if (contains? #{:0 :1 :2} status)
    (update-t! t-id :status status)
    {:error (str/join ["Status" status "doesn't exist, use the following only: open, in progress, done."])}))


(defn update-t-prior!
  [t-id prior]
  (if (contains? #{:0 :1 :2} prior)
    (update-t! t-id :prior prior)
    {:error (str/join ["Status" prior "doesn't exist, use the following only: open, in progress, done."])}))


(defn add-t-to-p!
  [p-id t-id]
  (if (and (pdb/p-exists? p-id) (t-exists? t-id))
    (swap! pdb/projects update-in [p-id :tickets] conj t-id)
    {:error (str/join ["Project with ID:" p-id " or ticket with ID:" t-id "doesn't exist."])}))


(defn remove-t-from-p!
  [p-id t-id]
  (if (and (pdb/p-exists? p-id) (t-exists? t-id))
    (swap! pdb/projects update-in [p-id :tickets] disj t-id)
    {:error (str/join ["Project with ID:" p-id " or ticket with ID:" t-id " doesn't exist,
    so nothing to update!"])}))


(defn remove-all-t-from-p!
  [p-id]
  (if (pdb/p-exists? p-id)
    (swap! pdb/projects assoc-in [p-id :tickets] #{})
    {:error (str/join ["Project with ID:" p-id "doesn't exist,
    so nothing to update!"])}))



;;;; ------------ DELETE --------------
;
(defn delete-t-of-p!
  "Deletes all tickets of selected project if any"
  ([p-id]
   (delete-t-of-p! pdb/projects tickets p-id))
  ([projects tickets p-id]
   (if (pdb/p-exists? projects p-id)
     (swap! tickets #(apply dissoc @tickets (get-tt-of-p p-id)))
     {:error (str/join ["Project with ID:" p-id "doesn't exist, so nothing to delete"])})))


(defn delete-t!
  "Delete single ticket from tickets and from corresponding project"
  [t-id]
  (if (t-exists? t-id)
    (let [p-id ((@tickets t-id) :project)]
      (remove-t-from-p! p-id t-id)
      (swap! tickets dissoc t-id))
    {:error (str/join ["Ticket with ID:" t-id "doesn't exist, so nothing to delete!"])}))

