DROP DATABASE IF EXISTS crowdfunding_ae;
CREATE DATABASE crowdfunding_ae ENCODING 'UTF-8';

DROP DATABASE IF EXISTS crowdfunding_ae_test;
CREATE DATABASE crowdfunding_ae_test ENCODING 'UTF-8';

DROP USER IF EXISTS crowdfunding_ae;
CREATE USER crowdfunding_ae WITH PASSWORD 'password';

DROP USER IF EXISTS crowdfunding_ae_test;
CREATE USER crowdfunding_ae_test WITH PASSWORD 'password';
