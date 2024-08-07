#!/usr/bin/env bash

#0: Sjekk at du er på main
current_branch=$(git branch | grep \* | cut -d ' ' -f2)
main_branch="main"

if ! [ "$current_branch" == "$main_branch" ]; then
    printf "Må være på main-branch for å tagge. Avslutter script\n"
    exit 1
fi

#1: Hent nåværende versjon/tag
git fetch --prune --tags

version=$(git describe --abbrev=0 --tags)
current_major=${version:1:1}
current_minor=${version:3}
printf "Nåværende versjon: $version\n"

#2: Hent major/minor-flagg fra kommandolinjen og bump i henhold til det
while getopts ":Mm" opt
do
  case $opt in
    M ) major='true';;
    m ) minor='true';;
  esac
done

shift $(($OPTIND - 1))

if [ -z $major ] && [ -z $minor ]; then
    printf "usage: Spesifiser om du vil bumpe major og/eller minor med hhv flaggene -M og -m.\n"
    printf "Major/minor vil da bumpes med 1 fra siste release-tag.\n"
    exit 1
fi

new_major=$current_major
new_minor=$current_minor

if [ "$major" = true ]; then
    ((new_major++))
    new_minor=0
fi

if [ "$minor" = true ]; then
    ((new_minor++))
fi

next_version="v$new_major.$new_minor"
printf "Neste versjon blir: $next_version\n"

#3: Tag git commit med ny versjon
git tag -a $next_version -m "Release av versjon $next_version"

#4: Push tag
git push origin $next_version
