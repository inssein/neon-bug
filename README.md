# Neon Bug

Sample rest calls in `sample.rest` if you want to walk through each call until it breaks.

I haven't yet figured out what is wrong, it feels like after it encounters a certain row, the timestamps become weird, until you acquire a new connection.

## The Bug
At some point, neon starts responding with `2000-01-01T00:00:00` as the timestamp for a row in the `idea` table, but as you can see from `src/main/resources/schema.sql`, all rows have valid timestamps.

## Reproduction
1. Build and run the docker container below
    ```
    docker build -t neon-bug .
    docker run -p 8080:8080 -e DB_URL=jdbc:postgresql://<host>.cloud.neon.tech/<db> -e DB_USER=<user> -e DB_PASS=<pass> neon-bug
    ```
2. Run the below curl command to fetch a row with `created_at` of `2000-01-01T00:00:00`
    ```
    curl localhost:8080/bug
    ```
3. Running the above command again will return a different row.
