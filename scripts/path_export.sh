if ! grep -q "export vm5277=" ~/.bashrc; then
  echo -e "\nexport vm5277=$(dirname $(pwd))" >> ~/.bashrc
  echo 'export PATH="$PATH:$vm5277/bin"' >> ~/.bashrc
fi

if ! grep -q "export vm5277=" ~/.xsessionrc; then
  echo -e "\nexport vm5277=$(dirname $(pwd))" >> ~/.xsessionrc
  echo 'export PATH="$PATH:$vm5277/bin"' >> ~/.xsessionrc
fi
