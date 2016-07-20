(ns jira.projects.db
  (:require
    [jira.users.db :as udb]
    [clojure.string :as str]))


;(enable-console-print!)



;;;; ------- INIT ------------

(defonce projects (atom {}))
(defonce latest-p-id (atom 0))
(defonce selected-p (atom nil))



;;;; -------- GET ------------

(defn next-p-id [] (swap! latest-p-id inc))

(defn select-p [p-id] (reset! selected-p p-id))

(defn count-projects [] (count @projects))

(defn empty-p
  "Creates a new prj to be inserted into the collection"
  [u-name p-title descr]
  {:title       p-title
   :description descr
   :author      u-name
   :tickets     #{}
   :users       #{u-name}})


(defn p-title->id
  "Returns prj id for given title"
  [prj-title]
  (first (filter #(= ((@projects %) :title) prj-title) (keys @projects))))


(defn p-title-unique?
  "Checks if project title already used"
  [p-title]
  (nil? (p-title->id p-title)))


(defn p-exists?
  "Checks if project with such id exists. projects are an atom."
  ([p-id]
   (p-exists? projects p-id))
  ([projects p-id]
   (some #(= p-id %) (keys (deref projects)))))



(defn get-p
  "Returns single projects details for the project ID"
  ([p-id]
   (get-p projects p-id))
  ([projects p-id]
   (get (deref projects) p-id
     {:error "Sorry, no such project"})))


(defn get-u-projects
  "Returns all Projects ids accessible by User"
  [u-name]
  (if (udb/u-exists? u-name)
    (for [p-id (keys @projects)
          :let [u-projects (conj p-id)]
          :when (contains? ((@projects p-id) :users) u-name)]
      u-projects)
    {:error (str/join ["User with login" u-name "doesn't exist"])}))


(defn get-p-title
  [p-id]
  (if (p-exists? p-id)
    ((@projects p-id) :title)
    {:error (str/join ["Project with ID" p-id "doesn't exist"])}))

(defn get-p-descr
  [p-id]
  (if (p-exists? p-id)
    ((@projects p-id) :description)
    {:error (str/join ["Project with ID" p-id "doesn't exist"])}))


(defn who-is-author
  [p-id]
  (if (p-exists? p-id)
    ((@projects p-id) :author)
    {:error (str/join ["Project with ID:" p-id "doesn't exist"])}))


(defn count-p-tickets
  [p-id]
  (if (p-exists? p-id)
    (count (jira.tickets.db/get-tt-of-p p-id))
    {:error "Project with ID: doesn't exisst."}))



;;;; ---------- CREATE ----------

(defn create-p!
  ([p-title]
   (create-p! p-title ""))
  ([p-title descr]
   (create-p! projects p-title descr))
  ([projects p-title descr]
   (let [id (next-p-id)]
     (cond
       (not (p-title-unique? p-title))
       {:error (str/join ["Project with title: " p-title " has been already created! Choose another."])}
       (empty? p-title)
       {:error "Project title should not be empty. Please specify!"}
       :else
       (do
         (swap! projects assoc id (empty-p @udb/current-u p-title descr))
         (select-p id))))))



;;;; ------------ UPDATE --------------

(defn update-p-title!
  [p-id new-title]
  (cond
    (not (p-exists? p-id))
    {:error (str/join ["Project with ID:" p-id "doesn't exist, so nothing to update!"])}
    (not (p-title-unique? new-title))
    {:error (str/join ["Project title: " new-title " already used.
                        Use unique projects titles please"])}
    :else
    (swap! projects assoc-in [p-id :title] new-title)))


(defn update-p-descr!
  [p-id new-descr]
  (if (p-exists? p-id)
    (swap! projects assoc-in [p-id :description] new-descr)
    {:error (str/join ["Project with ID:" p-id "doesn't exist, so nothing to update!"])}))


(defn add-u-to-p!
  [p-id user-name]
  (if (and (p-exists? p-id) (udb/u-exists? user-name))
    (swap! projects update-in [p-id :users] conj user-name)
    {:error (str/join ["Project with ID:" p-id " or user" user-name
                        "doesn't exist, so nothing to update!"])}))


(defn remove-u-from-p!
  "Removes User from Project, so Project becomes unaccessible by that User"
  [p-id user-name]
  (if (and (p-exists? p-id) (udb/u-exists? user-name) (not= (who-is-author p-id) user-name))
    (swap! projects update-in [p-id :users] disj user-name)
    {:error (str/join ["Project with ID:" p-id " or user" user-name "doesn't exist, so nothing to update!
            also can't remove author from project."])}))



;;;; ---------- DELETE ----------------



(defn delete-p!
  "Deletes a project and its tickets if any"
  ([p-id]
   (delete-p! projects p-id))
  ([projects p-id]
   (if (p-exists? p-id)
     (do (jira.tickets.db/delete-t-of-p! p-id)
         (swap! projects dissoc p-id)
         (reset! selected-p (-> @udb/current-u
                              get-u-projects
                              first)))
     {:error (str/join ["Project with ID:" p-id "doesn't exist, so nothing to delete"])})))
